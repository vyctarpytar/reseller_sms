import { useEffect, useState } from "react";

/**
 * True when the viewport is below the desktop breakpoint — the same 1024px line
 * where the sidebar collapses into the MobileDrawer. Mobile-only logic keys off
 * this (e.g. ResponsiveTable's scroll override); desktop is never affected
 * because the hook is always false at >= 1024px.
 */
const MOBILE_QUERY = "(max-width: 1023px)";

export default function useIsMobile() {
  const [isMobile, setIsMobile] = useState(() =>
    typeof window !== "undefined" && window.matchMedia
      ? window.matchMedia(MOBILE_QUERY).matches
      : false
  );

  useEffect(() => {
    if (typeof window === "undefined" || !window.matchMedia) return;
    const mql = window.matchMedia(MOBILE_QUERY);
    const onChange = (e) => setIsMobile(e.matches);
    setIsMobile(mql.matches);
    // addEventListener is the modern API; addListener is the Safari <14 fallback.
    if (mql.addEventListener) mql.addEventListener("change", onChange);
    else mql.addListener(onChange);
    return () => {
      if (mql.removeEventListener) mql.removeEventListener("change", onChange);
      else mql.removeListener(onChange);
    };
  }, []);

  return isMobile;
}
