// SMPP 3.4 caps these, not us: system_id is a 16-octet C-string (15 chars) and
// password a 9-octet one (8 chars). The backend enforces the same limits.
export const SYSTEM_ID_MAX = 15;
export const PASSWORD_MAX = 8;
export const SYSTEM_ID_PATTERN = /^[A-Za-z0-9_.-]{3,15}$/;
export const PASSWORD_PATTERN = /^[\x21-\x7E]{6,8}$/;
// Credentials an upstream SMSC issued to us: only the protocol's own limits apply.
export const UPSTREAM_SYSTEM_ID_PATTERN = /^[\x21-\x7E]{1,15}$/;
export const UPSTREAM_PASSWORD_PATTERN = /^[\x21-\x7E]{1,8}$/;

// No 0/O, 1/l/I: these get read out over the phone and retyped into an SMSC console.
const PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

/**
 * Username derived from the account name: "World Interconnect Ltd" -> "world_interconn".
 * `taken` is the usernames already on screen; a clash gets a numeric suffix. That only
 * covers this reseller's connections — the backend has the final say on uniqueness.
 */
export function suggestSystemId(accName = "", taken = []) {
  let base = String(accName)
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "");
  if (base.length < 3) base = `smpp_${base}`.replace(/_+$/, "");
  const used = new Set(taken.map((t) => String(t).toLowerCase()));

  const fit = (suffix) =>
    base.slice(0, SYSTEM_ID_MAX - suffix.length).replace(/_+$/, "") + suffix;

  let candidate = fit("");
  for (let n = 2; used.has(candidate); n++) candidate = fit(`_${n}`);
  return candidate;
}

export function generatePassword() {
  const bytes = new Uint32Array(PASSWORD_MAX);
  window.crypto.getRandomValues(bytes);
  return Array.from(bytes, (b) => PASSWORD_ALPHABET[b % PASSWORD_ALPHABET.length]).join("");
}

export const BIND_TYPES = [
  { value: "TRANSCEIVER", label: "Transceiver (send and receive)" },
  { value: "TRANSMITTER", label: "Transmitter (send only)" },
  { value: "RECEIVER", label: "Receiver (delivery reports only)" },
];

export const CONNECTION_TYPES = [
  { value: "SERVER", label: "Client connects to us (we issue the credentials)" },
  { value: "CLIENT", label: "We connect to an upstream SMSC (they issued the credentials)" },
];

export const isUpstream = (c) => c?.connectionType === "CLIENT";

/** The block a reseller pastes to their client. */
export function connectionDetailsText(c) {
  return [
    `Host: ${c?.host}`,
    `Port: ${c?.port}`,
    `System ID: ${c?.systemId}`,
    `Password: ${c?.password}`,
    `Bind mode: ${c?.bindType}`,
    "Protocol: SMPP v3.4",
  ].join("\n");
}
