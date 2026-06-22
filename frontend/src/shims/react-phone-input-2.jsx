// react-phone-input-2 ships as CJS (lib/lib.js, no `module`/`exports` fields).
// Rolldown (Vite 8 production bundler) doesn't auto-unwrap the CJS .default,
// so `import PhoneInput from "react-phone-input-2"` resolves to the module
// object { default: Fn } instead of the component — React error #130 on render.
// This shim imports the real file by its exact path (bypassing the alias) and
// performs the interop unwrap so every consumer gets the component function.
import PhoneInputLib from "../../node_modules/react-phone-input-2/lib/lib.js";
export default PhoneInputLib.default ?? PhoneInputLib;
