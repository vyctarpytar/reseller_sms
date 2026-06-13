import { Breadcrumb } from "antd";
import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import {
  CopyOutlined,
  CheckOutlined,
  EyeOutlined,
  EyeInvisibleOutlined,
  KeyOutlined,
  LockOutlined,
} from "@ant-design/icons";
import { fetchSandbox } from "../../features/sandbox/sandboxSlice";
import { dateForHumans } from "../../utils";

// ── Shared dark-surface styling (kept in-file: this page is the only consumer) ──
const DARK = "#13161D";
const codeOuter = {
  background: DARK,
  boxShadow: "inset 0 0 0 1px rgba(255,255,255,0.06)",
};
const monoFamily =
  '"SF Mono","JetBrains Mono","Fira Code",Menlo,Consolas,monospace';

// Render a code/cURL string line-by-line, dimming `#` comment lines so annotated
// examples (e.g. the "# Delivery report POSTed to your callbackUrl" block) read
// like real terminal output. Never mutates the string — only colors it.
function renderCode(code) {
  const lines = String(code ?? "").split("\n");
  return lines.map((line, i) => {
    const isComment = line.trimStart().startsWith("#");
    return (
      <span
        key={i}
        className="block"
        style={{ color: isComment ? "rgba(255,255,255,0.42)" : "rgba(255,255,255,0.88)" }}
      >
        {line === "" ? " " : line}
      </span>
    );
  });
}

// Per-instance copy control with local "Copied" feedback (no shared toast, so two
// blocks give independent feedback). Fixed min-width => no layout shift on morph.
function CopyButton({ text, variant = "dark", disabled = false }) {
  const [copied, setCopied] = useState(false);
  const [hover, setHover] = useState(false);
  const dark = variant === "dark";

  const onCopy = () => {
    if (disabled || !text) return;
    try {
      navigator?.clipboard
        ?.writeText(text)
        ?.then(() => {
          setCopied(true);
          setTimeout(() => setCopied(false), 1600);
        })
        .catch(() => {});
    } catch (e) {
      /* clipboard unavailable on insecure origins — silently no-op */
    }
  };

  return (
    <button
      type="button"
      onClick={onCopy}
      disabled={disabled}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      aria-label="Copy to clipboard"
      className={`inline-flex items-center justify-center gap-1.5 text-[12px] font-medium px-2 py-1 rounded min-w-[74px] transition-colors ${
        disabled ? "opacity-40 cursor-not-allowed" : "cursor-pointer"
      } ${dark ? "" : copied ? "" : "text-muted hover:text-accent"}`}
      style={
        dark
          ? {
              color: copied ? "#8be9a3" : "rgba(255,255,255,0.6)",
              background:
                hover && !disabled && !copied ? "rgba(255,255,255,0.08)" : "transparent",
            }
          : copied
          ? { color: "#047857" }
          : undefined
      }
    >
      {copied ? <CheckOutlined /> : <CopyOutlined />}
      {copied ? "Copied" : "Copy"}
    </button>
  );
}

// The signature dark code island: header bar (lang tag + copy) over a monospace,
// horizontally-scrolling body. `loading` renders shimmer lines in the same shell.
function CodeBlock({ code, lang = "cURL", loading = false }) {
  return (
    <div className="rounded-lg overflow-hidden" style={codeOuter}>
      <div
        className="flex items-center justify-between px-3 py-2"
        style={{ borderBottom: "1px solid rgba(255,255,255,0.08)" }}
      >
        <span
          className="font-mono text-[11px] uppercase tracking-wider"
          style={{ color: "rgba(255,255,255,0.4)" }}
        >
          {lang}
        </span>
        <CopyButton text={code} variant="dark" disabled={loading} />
      </div>
      {loading ? (
        <div className="px-4 py-3.5 space-y-2.5">
          {[90, 70, 55].map((w) => (
            <div
              key={w}
              className="h-3 rounded animate-pulse"
              style={{ width: `${w}%`, background: "rgba(255,255,255,0.06)" }}
            />
          ))}
        </div>
      ) : (
        <pre
          className="px-4 py-3.5 text-[12.5px] leading-relaxed overflow-x-auto"
          style={{ whiteSpace: "pre", tabSize: 2, fontFamily: monoFamily, margin: 0 }}
        >
          <code>{renderCode(code)}</code>
        </pre>
      )}
    </div>
  );
}

function Shimmer({ w = "60%", dark = true }) {
  return (
    <div
      className="h-4 rounded animate-pulse"
      style={{ width: w, background: dark ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.06)" }}
    />
  );
}

// Masked API key with reveal toggle. Copy ALWAYS sends the real key, never the dots.
function KeyField({ apiKey, ready }) {
  const [revealed, setRevealed] = useState(false);
  return (
    <div className="flex items-center gap-2">
      {ready ? (
        <span
          className="font-mono text-sm truncate flex-1 min-w-0"
          style={{ color: "rgba(255,255,255,0.9)" }}
        >
          {revealed ? apiKey : "•".repeat(32)}
        </span>
      ) : (
        <div className="flex-1 min-w-0">
          <Shimmer w="66%" />
        </div>
      )}
      <button
        type="button"
        onClick={() => setRevealed((v) => !v)}
        disabled={!ready}
        aria-label={revealed ? "Hide API key" : "Show API key"}
        className={`p-1 rounded transition-colors ${
          ready ? "cursor-pointer hover:bg-white/5" : "opacity-40 cursor-not-allowed"
        }`}
        style={{ color: "rgba(255,255,255,0.55)" }}
      >
        {revealed ? <EyeInvisibleOutlined /> : <EyeOutlined />}
      </button>
      <CopyButton text={apiKey} variant="dark" disabled={!ready} />
    </div>
  );
}

// One-line copyable mono value on the dark terminal (used for the base URL).
function DarkCopyValue({ value, ready }) {
  return (
    <div className="flex items-center gap-2">
      {ready ? (
        <span
          className="font-mono text-sm truncate flex-1 min-w-0"
          style={{ color: "rgba(255,255,255,0.85)" }}
        >
          {value}
        </span>
      ) : (
        <div className="flex-1 min-w-0">
          <Shimmer w="60%" />
        </div>
      )}
      <CopyButton text={value} variant="dark" disabled={!ready} />
    </div>
  );
}

function CredRow({ label, span2, children }) {
  return (
    <div className={span2 ? "sm:col-span-2" : ""}>
      <div
        className="text-[10px] uppercase tracking-widest mb-1"
        style={{ color: "rgba(255,255,255,0.35)" }}
      >
        {label}
      </div>
      {children}
    </div>
  );
}

function MetaValue({ value, ready }) {
  if (!ready) return <Shimmer w="55%" />;
  return (
    <div className="font-mono text-sm truncate" style={{ color: "rgba(255,255,255,0.8)" }}>
      {value || "—"}
    </div>
  );
}

function StatusPill({ active, ready }) {
  let dot = "rgba(255,255,255,0.35)";
  let text = "Loading…";
  let style = { color: "rgba(255,255,255,0.4)" };
  if (ready && active) {
    dot = "#D96C3B";
    text = "Active";
    style = { background: "rgba(217,108,59,0.15)", color: "#e8a07f" };
  } else if (ready && !active) {
    dot = "rgba(255,255,255,0.3)";
    text = "Inactive";
    style = { color: "rgba(255,255,255,0.4)" };
  }
  return (
    <span
      className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-medium"
      style={style}
    >
      <span className="rounded-full" style={{ width: 8, height: 8, background: dot }} />
      {text}
    </span>
  );
}

// The only place color carries meaning: terracotta = POST (mutation), brown = GET (read).
function MethodBadge({ method }) {
  const post = method === "POST";
  const style = post
    ? {
        background: "rgba(217,108,59,0.12)",
        color: "#b8562a",
        boxShadow: "inset 0 0 0 1px rgba(217,108,59,0.30)",
      }
    : {
        background: "rgba(105,71,46,0.10)",
        color: "#69472E",
        boxShadow: "inset 0 0 0 1px rgba(105,71,46,0.22)",
      };
  return (
    <span
      className="inline-flex items-center justify-center font-mono text-[11px] font-bold tracking-wide px-2.5 py-1 rounded-md min-w-[3.25rem]"
      style={style}
    >
      {method}
    </span>
  );
}

function PathLabel({ path }) {
  return (
    <code className="font-mono text-sm truncate">
      <span className="text-muted">…/sandbox</span>
      <span className="text-primary font-semibold">{path}</span>
    </code>
  );
}

function SectionHead({ title, count }) {
  return (
    <div className="flex items-baseline gap-3 mb-4">
      <h2 className="text-lg font-semibold text-primary">{title}</h2>
      {count ? <span className="text-xs text-muted">{count}</span> : null}
    </div>
  );
}

function EndpointCard({
  method,
  path,
  description,
  requestCode,
  requestLang = "cURL",
  responseCode,
  statusLegend,
  requestLoading = false,
  responseLoading = false,
}) {
  return (
    <div className="card card-hover">
      <div className="flex items-center gap-3">
        <MethodBadge method={method} />
        <PathLabel path={path} />
        <div className="ml-auto flex-none">
          <CopyButton text={requestCode} variant="light" disabled={requestLoading} />
        </div>
      </div>
      <p className="text-sm text-muted leading-relaxed mt-2 mb-4">{description}</p>
      {statusLegend ? (
        <div className="flex flex-wrap items-center gap-2 mb-4">{statusLegend}</div>
      ) : null}
      <CodeBlock code={requestCode} lang={requestLang} loading={requestLoading} />
      {responseCode !== undefined && (
        <>
          <div className="text-[10px] uppercase tracking-widest text-muted mb-2 mt-4">
            Sample response
          </div>
          <CodeBlock code={responseCode} lang="JSON" loading={responseLoading} />
        </>
      )}
    </div>
  );
}

const QUICK_START = [
  { n: 1, t: "Grab your key", d: "Copy the X-API-KEY above." },
  { n: 2, t: "Check your balance", d: "GET /balance returns KES + units." },
  { n: 3, t: "Send your first SMS", d: "POST /single-sms to a recipient." },
];

function SandBox() {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { sandData } = useSelector((state) => state.sand);

  useEffect(() => {
    dispatch(fetchSandbox());
  }, [dispatch]);

  // sandData ships as [] (truthy) and loads async — "loaded" is key presence, not !sandData.
  const ready = !!(sandData && !Array.isArray(sandData) && sandData.apiKey);

  const formatJson = (jsonString) => {
    try {
      return JSON?.stringify(JSON?.parse(jsonString), null, 2);
    } catch (error) {
      return jsonString;
    }
  };

  // Account & top-up docs are built client-side from the issued key so they stay in sync with
  // it. The base tracks the same host as the single-sms endpoint. Examples render even pre-fetch
  // via the 'YOUR_API_KEY' fallback, so this half of the page is always complete.
  const apiBase = (
    sandData?.apiEndPoint || "https://backend.synqafrica.co.ke/api/v2/sandbox/single-sms"
  ).replace(/\/single-sms$/, "");
  const apiKeyVal = sandData?.apiKey || "YOUR_API_KEY";

  const balanceCurl = `curl --request GET \\
  --url ${apiBase}/balance \\
  --header 'X-API-KEY: ${apiKeyVal}'`;

  const loadCurl = `curl --request POST \\
  --url ${apiBase}/load \\
  --header 'Content-Type: application/json' \\
  --header 'X-API-KEY: ${apiKeyVal}' \\
  --data '{
  "smsPayAmount": 1000,
  "smsPayerMobileNumber": "254712345678",
  "smsLoadingMethod": "MONEY"
}'`;

  const statusCurl = `curl --request GET \\
  --url ${apiBase}/invoice-status/SMS0001ABCD \\
  --header 'X-API-KEY: ${apiKeyVal}'`;

  const balanceResp = `{
  "success": true,
  "data": { "result": {
    "accName": "Your Company",
    "accStatus": "ACTIVE",
    "accBalance": 142.00,
    "accRate": 0.50,
    "accUnits": 284
  } },
  "status": 200
}`;

  const loadResp = `{
  "success": true,
  "messages": { "message": "STK pop for amount 1000 to code SMS0001ABCD" },
  "data": { "result": {
    "invoCode": "SMS0001ABCD",
    "invoStatus": "PENDING_PAYMENT",
    "invoAmount": 1000
  } },
  "status": 200
}`;

  const statusResp = `{
  "success": true,
  "data": { "result": {
    "invoiceCode": "SMS0001ABCD",
    "status": "PAID",
    "paid": true,
    "amount": 1000,
    "failureReason": null,
    "mpesaReceipt": "TXXXXXXXXX"
  } },
  "status": 200
}`;

  return (
    <div className="overflow-y-auto h-full w-full bg-surface font-dmSans text-ink lg:px-10 px-3 pb-24">
      <div className="max-w-5xl mx-auto">
        {/* Breadcrumb */}
        <div className="pt-10">
          <Breadcrumb
            items={[
              {
                title: (
                  <span
                    className="text-xs text-muted cursor-pointer font-dmSans"
                    onClick={() => navigate(-1)}
                  >
                    Sandbox
                  </span>
                ),
              },
              {
                title: (
                  <span className="text-xs text-primary font-medium font-dmSans">
                    Developer sandbox
                  </span>
                ),
              },
            ]}
          />
        </div>

        {/* Header */}
        <div className="mt-1">
          <span className="sa-eyebrow">Developer</span>
          <h1 className="sa-title !text-3xl !normal-case mt-1">Sandbox console</h1>
          <p className="sa-subtitle mt-2 max-w-2xl leading-relaxed">
            Build and test against your sandbox key. Every request authenticates with the{" "}
            <code className="font-mono text-[12px] px-1.5 py-0.5 rounded bg-black/[0.06] text-ink">
              X-API-KEY
            </code>{" "}
            header.
          </p>
        </div>

        {/* Credentials terminal — the signature hero */}
        <div
          className="mt-8 rounded-card overflow-hidden shadow-lift"
          style={{ background: DARK }}
        >
          <div
            className="flex items-center gap-2 px-4 py-2.5"
            style={{
              background: "rgba(255,255,255,0.04)",
              borderBottom: "1px solid rgba(255,255,255,0.08)",
            }}
          >
            <span className="rounded-full opacity-80" style={{ width: 10, height: 10, background: "#ff5f56" }} />
            <span className="rounded-full opacity-80" style={{ width: 10, height: 10, background: "#ffbd2e" }} />
            <span className="rounded-full opacity-80" style={{ width: 10, height: 10, background: "#27c93f" }} />
            <span className="ml-2 font-mono text-[12px]" style={{ color: "rgba(255,255,255,0.4)" }}>
              sandbox — credentials
            </span>
            <span className="ml-auto">
              <StatusPill active={!!sandData?.active} ready={ready} />
            </span>
          </div>

          <div className="p-5 sm:p-6 grid grid-cols-1 sm:grid-cols-2 gap-y-5 gap-x-8">
            <CredRow label="API key" span2>
              <KeyField apiKey={sandData?.apiKey} ready={ready} />
            </CredRow>
            <CredRow label="Base URL" span2>
              <DarkCopyValue value={apiBase} ready />
            </CredRow>
            <div className="sm:col-span-2 h-px" style={{ background: "rgba(255,255,255,0.08)" }} />
            <CredRow label="Client name">
              <MetaValue value={sandData?.clientName} ready={ready} />
            </CredRow>
            <CredRow label="Created">
              <MetaValue
                value={sandData?.createdDate ? dateForHumans(sandData.createdDate) : "—"}
                ready={ready}
              />
            </CredRow>
            <CredRow label="Expires">
              <MetaValue
                value={sandData?.expirationDate ? dateForHumans(sandData.expirationDate) : "—"}
                ready={ready}
              />
            </CredRow>
          </div>
        </div>

        {/* Authentication contract */}
        <div
          className="mt-4 rounded-card p-4 flex items-start gap-3"
          style={{
            background: "rgba(217,108,59,0.06)",
            boxShadow: "inset 0 0 0 1px rgba(217,108,59,0.18)",
          }}
        >
          <KeyOutlined style={{ color: "#D96C3B", fontSize: 18, marginTop: 2 }} />
          <div className="flex-1 min-w-0">
            <div className="text-sm text-ink font-medium">Authentication</div>
            <p className="text-[13px] text-muted mt-0.5 mb-2 leading-relaxed">
              Send your key as the{" "}
              <code className="font-mono text-[12px] px-1 py-0.5 rounded bg-black/[0.06] text-ink">
                X-API-KEY
              </code>{" "}
              header on every request to{" "}
              <span className="font-mono text-[12px] text-ink break-all">{apiBase}</span>.
            </p>
            <div className="flex items-center gap-2 rounded-lg px-3 py-2" style={codeOuter}>
              <span
                className="font-mono text-[12.5px] truncate flex-1 min-w-0"
                style={{ color: "rgba(255,255,255,0.85)" }}
              >
                X-API-KEY: {apiKeyVal}
              </span>
              <CopyButton text={`X-API-KEY: ${apiKeyVal}`} variant="dark" />
            </div>
          </div>
        </div>

        {/* Quick start */}
        <div className="mt-6 card !p-0 overflow-hidden">
          <div className="px-6 py-3 border-b border-border text-sm font-semibold text-primary">
            Quick start
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 divide-y md:divide-y-0 md:divide-x divide-border">
            {QUICK_START.map((s) => (
              <div key={s.n} className="p-5 flex gap-3">
                <span
                  className="flex-none w-7 h-7 rounded-full text-accent text-sm font-bold grid place-items-center"
                  style={{ background: "rgba(217,108,59,0.10)" }}
                >
                  {s.n}
                </span>
                <div>
                  <div className="text-sm font-semibold text-ink">{s.t}</div>
                  <div className="text-[13px] text-muted mt-0.5">{s.d}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Send SMS */}
        <div className="mt-10">
          <SectionHead title="Send SMS" count="2 endpoints" />
          <div className="space-y-5">
            <EndpointCard
              method="POST"
              path="/single-sms"
              description="Send a single message to one recipient. Delivery reports are POSTed to your callbackUrl."
              requestCode={sandData?.apiPayload}
              requestLoading={!sandData?.apiPayload}
              responseCode={sandData?.apiResponse ? formatJson(sandData.apiResponse) : ""}
              responseLoading={!sandData?.apiResponse}
            />
            <EndpointCard
              method="POST"
              path="/bulk-sms"
              description="Send the same message to many recipients at once. Returns the same response shape as /single-sms."
              requestCode={sandData?.apiPayloadMultiple}
              requestLoading={!sandData?.apiPayloadMultiple}
            />
          </div>
        </div>

        {/* Account & top-up */}
        <div className="mt-10">
          <SectionHead title="Account & top-up" count="3 endpoints" />
          <div className="space-y-5">
            <EndpointCard
              method="GET"
              path="/balance"
              description="Returns the account SMS balance (KES) and the equivalent number of units."
              requestCode={balanceCurl}
              responseCode={balanceResp}
            />
            <EndpointCard
              method="POST"
              path="/load"
              description={
                <>
                  Creates an invoice and sends an M-Pesa STK push to{" "}
                  <b className="text-ink font-semibold">smsPayerMobileNumber</b>. The balance is
                  credited once the payment settles — poll the returned{" "}
                  <b className="text-ink font-semibold">invoCode</b> with the invoice-status
                  endpoint to know when it&apos;s done.
                </>
              }
              statusLegend={<span className="badge-pending">PENDING_PAYMENT</span>}
              requestCode={loadCurl}
              responseCode={loadResp}
            />
            <EndpointCard
              method="GET"
              path="/invoice-status/{invoiceCode}"
              description={
                <>
                  Polls a load invoice. <b className="text-ink font-semibold">paid: true</b> (status
                  PAID) means the top-up is complete. Terminal failure statuses are FAILED,
                  CANCELLED and EXPIRED.
                </>
              }
              statusLegend={
                <>
                  <span className="badge-approved">PAID</span>
                  <span className="badge-pending">PENDING_PAYMENT</span>
                  <span className="badge-rejected">FAILED</span>
                  <span className="badge-neutral">CANCELLED</span>
                  <span className="badge-neutral">EXPIRED</span>
                </>
              }
              requestCode={statusCurl}
              responseCode={statusResp}
            />
          </div>
        </div>

        {/* Footer secret note */}
        <div className="mt-8 text-[13px] text-muted flex items-center gap-2">
          <LockOutlined />
          Keep your API key secret — treat it like a password and never embed it in client-side
          code.
        </div>
      </div>
    </div>
  );
}

export default SandBox;
