package com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest;

import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestLoginRequest;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestOAuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SafaricomRestAuthService {

    // Written only under the instance lock (loginFresh / refreshOnUnauthorized / scheduledTokenRefresh),
    // read lock-free on the hot path. volatile guarantees visibility of the latest token.
    private volatile String accessToken;
    private volatile String refreshToken;

    private final SafaricomRestProperties safaricomRestProperties;
    private final SafaricomRestInterface safaricomRestInterface;

    /** Hot path: return the cached token; perform a (single-flight) login only if none is cached. */
    public String getAccessToken() throws IOException {
        String token = accessToken;
        if (!TextUtils.isEmpty(token)) {
            return token;
        }
        return loginFresh();
    }

    /**
     * 401 recovery for a send. Re-logs in only if the cached token still matches the one that just
     * failed, so concurrent senders that all hit 401 with the same token share a single refresh
     * instead of stampeding the login endpoint.
     *
     * @param failedToken the access token that the caller just got a 401 with
     * @return a fresh access token to retry with
     */
    public synchronized String refreshOnUnauthorized(String failedToken) throws IOException {
        String current = accessToken;
        if (!TextUtils.isEmpty(current) && !current.equals(failedToken)) {
            return current; // another thread already refreshed past the failed token
        }
        accessToken = null;
        return loginFresh();
    }

    /** Performs the actual login. Synchronized + double-checked so concurrent misses collapse into one call. */
    private synchronized String loginFresh() throws IOException {
        String current = accessToken;
        if (!TextUtils.isEmpty(current)) {
            return current; // filled in while we waited for the lock
        }

        SafaricomRestLoginRequest loginRequest = SafaricomRestLoginRequest.builder()
                .username(safaricomRestProperties.getUsername())
                .password(safaricomRestProperties.getPassword())
                .build();

        Response<SafaricomRestOAuthResponse> res = safaricomRestInterface.login(loginRequest).execute();
        if (res.isSuccessful() && res.body() != null && !TextUtils.isEmpty(res.body().getToken())) {
            accessToken = res.body().getToken();
            refreshToken = res.body().getRefreshToken();
            log.info("[DSDP] Access token obtained successfully");
            return accessToken;
        }
        throw new RuntimeException("[DSDP] Failed to obtain token — HTTP " + res.code());
    }

    // DSDP access token is valid ~24h; proactively rotate every 50 minutes.
    @Scheduled(cron = "0 */50 * * * *")
    public synchronized void scheduledTokenRefresh() {
        log.info("[DSDP] Scheduled token refresh");
        try {
            if (!TextUtils.isEmpty(refreshToken) && refreshViaRefreshToken()) {
                return;
            }
            // No usable refresh token (or it was rejected) -> full re-login.
            accessToken = null;
            loginFresh();
        } catch (Exception e) {
            // Keep the current (still-valid) token; 401-recovery on send covers unexpected expiry.
            log.error("[DSDP] Scheduled refresh failed; keeping current token", e);
        }
    }

    /** @return true if the access token was successfully renewed via the refresh token. */
    private boolean refreshViaRefreshToken() throws IOException {
        Response<SafaricomRestOAuthResponse> res =
                safaricomRestInterface.refreshToken("Bearer " + refreshToken).execute();
        if (res.isSuccessful() && res.body() != null && !TextUtils.isEmpty(res.body().getToken())) {
            accessToken = res.body().getToken();
            if (!TextUtils.isEmpty(res.body().getRefreshToken())) {
                refreshToken = res.body().getRefreshToken();
            }
            log.info("[DSDP] Access token refreshed via refresh token");
            return true;
        }
        log.warn("[DSDP] Refresh token rejected (HTTP {}), will fall back to full login", res.code());
        refreshToken = null;
        return false;
    }
}
