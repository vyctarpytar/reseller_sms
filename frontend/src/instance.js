import axios from "axios";
import toast from "react-hot-toast";
import { getTenantScope } from "./custom_hooks/useTenantScope";

const url = import.meta.env.VITE_API_BASE_URL;

const axiosInstance = axios.create({
  baseURL: url,
  headers: {
    "Content-Type": "application/json",
  },
});

axiosInstance.interceptors.request.use((config) => {
  // `skipTenantScope` opts a single call out entirely, for the few lists that must
  // stay platform-wide while a TOP user is drilled into a reseller — the reseller
  // switcher above all: GET api/v2/res narrows to `reseller_id` when it is sent, so
  // a stamped call returns only the reseller you are already in.
  if (config.skipTenantScope) return config;

  const { selectedOrg, selectedAccount } = getTenantScope();

  // Stamp every request with the globally selected tenant (reseller_id / account_id)
  // as a DEFAULT only. A caller that sets these explicitly — e.g. the wallet
  // statement's own reseller/account filters — must win, so we don't overwrite a
  // value the caller already provided.
  const params = { ...(config.params || {}) };
  if (params.reseller_id == null && selectedOrg) params.reseller_id = selectedOrg;
  if (params.account_id == null && selectedAccount) params.account_id = selectedAccount;
  config.params = params;

  return config;
});

// What to do once the session is known to be over. <Root> registers it, because it
// owns the store and the router and importing either here would be a cycle.
let onSessionEnded = () => {};
export function setSessionEndedHandler(handler) {
  onSessionEnded = handler;
}

const currentAuth = () => axiosInstance.defaults.headers.common["Authorization"];

// The backend answers 403 for two unrelated things: "you may not do this" (its own
// JSON envelope, success:false) and "this token is no good any more" (Spring
// Security: revoked by a newer login of the same user, expired, missing — a bare
// 403). An error it fails to handle also surfaces as a bare 403. Only the dead
// token ends the session, so a bare 403 / 401 is checked against an endpoint every
// signed-in user may call before anything is torn down. One check is shared by a
// burst of failures and its answer is reused for a few seconds.
let sessionCheck = null;
function sessionIsOver(auth) {
  if (sessionCheck?.auth !== auth) {
    const check = {
      auth,
      result: axios
        .get(`${url}/api/v1/menu`, { headers: { Authorization: auth } })
        .then(
          () => false,
          (e) => e?.response?.status === 401 || e?.response?.status === 403
        ),
    };
    check.result.finally(() =>
      setTimeout(() => {
        if (sessionCheck === check) sessionCheck = null;
      }, 5000)
    );
    sessionCheck = check;
  }
  return sessionCheck.result;
}

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const refused = status === 403 && error.response?.data?.success === false;

    if ((status === 401 || status === 403) && !refused) {
      const headers = error.config?.headers;
      const sent = headers?.get?.("Authorization") ?? headers?.Authorization;
      // A reply to a request from an earlier session says nothing about this one.
      if (sent && sent === currentAuth()) {
        sessionIsOver(sent).then((over) => {
          if (sent !== currentAuth()) return;
          if (over) onSessionEnded();
          else if (status === 403)
            toast.error("The server refused that request (403).", {
              id: "request-refused",
            });
        });
      }
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;
