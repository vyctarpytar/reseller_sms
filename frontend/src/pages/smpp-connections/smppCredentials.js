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

// What a client's SMSC team needs besides the credentials. These describe how
// the gateway (sms_smmp_gateway) behaves today — change them there first.
export const GATEWAY_SETTINGS = [
  ["Protocol", "SMPP v3.4"],
  ["Keep-alive", "enquire_link every 30 seconds"],
  ["Destination number", "International format, digits only (e.g. 2547XXXXXXXX)"],
  ["Message length", "160 characters, single part"],
  ["Encoding", "GSM 7-bit (data_coding 0)"],
  ["Delivery reports", "Set registered_delivery = 1; sent as deliver_sm on a transceiver or receiver bind"],
  ["Report statuses", "DELIVRD, UNDELIV, REJECTD, EXPIRED"],
  ["Report message id", "The message_id from submit_sm_resp (id: field and receipted_message_id)"],
  ["If you are offline", "Reports are held and retried for up to 48 hours"],
];

const BIND_LABELS = {
  TRANSCEIVER: "Transceiver",
  TRANSMITTER: "Transmitter",
  RECEIVER: "Receiver",
};

/** The credentials half: what is unique to this one connection. */
export function connectionCredentials(c) {
  return [
    ["Host", c?.host],
    ["Port", c?.port],
    ["System ID", c?.systemId],
    ["Password", c?.password],
    ["Bind mode", BIND_LABELS[c?.bindType] ?? c?.bindType],
  ];
}

/** The whole sheet as plain text, ready to paste into an email or an interconnect form. */
export function connectionDetailsText(c) {
  const lines = (rows) => rows.map(([label, value]) => `${label}: ${value}`);
  return [
    "SMPP CONNECTION DETAILS",
    "",
    ...lines(connectionCredentials(c)),
    "",
    "SETTINGS",
    "",
    ...lines(GATEWAY_SETTINGS),
  ].join("\n");
}
