package com.spa.smart_gate_springboot.utils;


import java.util.Set;

public class AppUtils {

    public static String CHANNEL_SMS = "SMS";


    //send bulk success status code
    public static String BULK_SMS_SEND_SUCCESS_STATUS_CODE = "SC0000";

    /**
     * Safaricom "SYSTEM ERROR" ({"keyword":"BULK","status":"SYSTEM ERROR","statusCode":"SC0029"}).
     * In practice this is what the carrier returns when the access token we are replaying has gone bad
     * on their side — the HTTP call itself still succeeds, so 401 recovery never fires. Treat it as a
     * signal to evict the cached token so the next attempt logs in fresh.
     */
    public static String BULK_SMS_SYSTEM_ERROR_STATUS_CODE = "SC0029";

    /**
     * Safaricom "QUOTA_EXPIRED" ({"keyword":"BULK","status":"QUOTA_EXPIRED","statusCode":"SC0012"}).
     * The quota Safaricom counts against is tied to the session behind the token, so a token we have
     * been holding for a while can exhaust it while a freshly issued one sends fine.
     */
    public static String BULK_SMS_QUOTA_EXPIRED_STATUS_CODE = "SC0012";

    /**
     * Carrier status codes whose remedy is a fresh login rather than a plain re-send. Each arrives on a
     * 2xx response — the HTTP layer sees nothing wrong — so the send paths test for them explicitly and
     * drop the cached token; the retry cron then re-sends against a newly minted one.
     */
    public static final Set<String> BULK_SMS_TOKEN_EVICT_STATUS_CODES =
            Set.of(BULK_SMS_SYSTEM_ERROR_STATUS_CODE, BULK_SMS_QUOTA_EXPIRED_STATUS_CODE);

    /** @return true if {@code statusCode} is one the carrier only clears after a fresh login. */
    public static boolean requiresTokenEviction(String statusCode) {
        return statusCode != null
                && BULK_SMS_TOKEN_EVICT_STATUS_CODES.stream().anyMatch(statusCode::equalsIgnoreCase);
    }

}
