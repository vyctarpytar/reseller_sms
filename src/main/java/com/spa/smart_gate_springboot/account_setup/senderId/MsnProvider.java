package com.spa.smart_gate_springboot.account_setup.senderId;

/**
 * Mobile service network a sender ID is registered on.
 *
 * <p>This is what routes a send: the Airtel gateway only ever picks a sender ID whose provider is
 * {@link #AIRTEL}, everything else stays on the Safaricom SDP/Daraja path. Rows created before the
 * column existed are backfilled to {@link #SAFARICOM} on boot
 * ({@code ShortCodeService.backfillMsnProvider}) — every sender ID predating it is a Safaricom one.
 *
 * <p>⚠ {@code ddl-auto: update} does NOT rebuild the Postgres {@code CHECK} constraint when a value
 * is added here — inserts of the new value then fail with {@code violates check constraint}. Adding
 * a provider means dropping {@code shortcode_sh_msn_provider_check} and
 * {@code shortcode_setup_sh_msn_provider_check} on every environment first.
 */
public enum MsnProvider {
    SAFARICOM, AIRTEL, TELKOM
}
