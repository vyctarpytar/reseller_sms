import React from "react";

/**
 * Drop-in replacement for the abandoned `material-icons-react` package.
 *
 * Why this exists: material-icons-react@1.0.4 dragged in its own vulnerable
 * subtree (React 15 → react-dom 15 → fbjs → isomorphic-fetch → node-fetch, plus
 * webfontloader), accounting for 6 of the repo's 7 high-severity npm-audit
 * advisories. None of that code is actually reachable for icon rendering, but it
 * trips audit. This shim emits the *identical* `<i class="material-icons">`
 * ligature markup the library produced, so we can delete the package.
 *
 * Wiring: a Vite alias (`resolve.alias` in vite.config.js) maps the bare
 * specifier `material-icons-react` to this file, so the 31 existing
 * `import MaterialIcon from "material-icons-react"` lines resolve here with NO
 * source changes. The `.material-icons` base class + the Material Icons webfont
 * are provided by the <link> added to index.html (same Google font the lib used).
 *
 * API parity with the original component:
 *  - icon   : ligature name rendered as the element's text (e.g. "filter_list")
 *  - size   : number/numeric-string → px; keyword (tiny/small/medium/large) →
 *             18/24/36/48; absent → the .material-icons default (24px)
 *  - color  : inline color (every call site passes one)
 *  - other props (onClick, title, …) pass straight through to the <i>
 *  - size/invert/inactive/style/className/preloader are consumed, not leaked to DOM
 */
const SIZE_KEYWORDS = { tiny: 18, small: 24, medium: 36, large: 48 };

export default function MaterialIcon({
  icon,
  size,
  color,
  className,
  style,
  invert, // lib-specific, accepted and ignored (kept off the DOM)
  inactive, // lib-specific, accepted and ignored
  preloader, // lib-specific, accepted and ignored
  ...rest
}) {
  let fontSize;
  if (size != null && size !== "") {
    const px = SIZE_KEYWORDS[size] ?? parseInt(size, 10);
    if (!Number.isNaN(px)) fontSize = `${px}px`;
  }

  return (
    <i
      {...rest}
      className={className ? `material-icons ${className}` : "material-icons"}
      style={{ ...(style || {}), color: color || undefined, fontSize }}
    >
      {icon}
    </i>
  );
}
