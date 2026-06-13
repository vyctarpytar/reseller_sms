package com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest;

import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestLoginRequest;
import com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto.SafaricomRestOAuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class SafaricomRestAuthService {

    /** Redis keys holding the current DSDP tokens (shared across instances/restarts). */
    private static final String ACCESS_KEY = "safaricom:dsdp:access_token";
    private static final String REFRESH_KEY = "safaricom:dsdp:refresh_token";

    /** Margin subtracted from a token's JWT {@code exp} so we rotate before the carrier expires it. */
    private static final Duration EXPIRY_MARGIN = Duration.ofMinutes(2);

    private final SafaricomRestProperties safaricomRestProperties;
    private final SafaricomRestInterface safaricomRestInterface;
    private final StringRedisTemplate redisTemplate;

    @Value("${safaricom.api.version:v1}")
    private String safaricomApiVersion;

    /** Fallback TTL when a token's {@code exp} claim can't be parsed (DSDP access token is valid ~24h). */
    @Value("${safaricom.dsdp.token.fallback-minutes:1440}")
    private int fallbackTtlMinutes;

    /** Hot path: return the cached token; perform a (single-flight) login only if none is cached. */
    public String getAccessToken() throws IOException {
        String token = getFromRedis(ACCESS_KEY);
        if (!TextUtils.isEmpty(token)) {
            log.info("[DSDP] Using cached access token from Redis");
            return token;
        }
        log.info("[DSDP] No cached token in Redis — performing fresh login");
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
        String current = getFromRedis(ACCESS_KEY);
        if (!TextUtils.isEmpty(current) && !current.equals(failedToken)) {
            return current; // another thread already refreshed past the failed token
        }
        evict(ACCESS_KEY);
        return loginFresh();
    }

    /** Performs the actual login. Synchronized + double-checked so concurrent misses collapse into one call. */
    private synchronized String loginFresh() throws IOException {
        String current = getFromRedis(ACCESS_KEY);
        if (!TextUtils.isEmpty(current)) {
            return current; // filled in while we waited for the lock
        }

        SafaricomRestLoginRequest loginRequest = SafaricomRestLoginRequest.builder()
                .username(safaricomRestProperties.getUsername())
                .password(safaricomRestProperties.getPassword())
                .build();

        Response<SafaricomRestOAuthResponse> res = safaricomRestInterface.login(loginRequest).execute();
        if (res.isSuccessful() && res.body() != null && !TextUtils.isEmpty(res.body().getToken())) {
            cacheTokens(res.body().getToken(), res.body().getRefreshToken());
            log.info("[DSDP] Access token obtained successfully");
            return res.body().getToken();
        }
        throw new RuntimeException("[DSDP] Failed to obtain token — HTTP " + res.code());
    }

    // DSDP access token is valid ~24h; proactively rotate every 50 minutes.
    @Scheduled(cron = "0 */50 * * * *")
    public synchronized void scheduledTokenRefresh() {
        // Only pre-warm the DSDP (v2) token when v2 is the selected provider; on v1 this would
        // otherwise hammer the Daraja-REST login endpoint for a token we never use.
        if (!"v2".equalsIgnoreCase(safaricomApiVersion)) {
            return;
        }
        log.info("[DSDP] Scheduled token refresh");
        try {
            if (refreshViaRefreshToken()) {
                return;
            }
            // No usable refresh token (or it was rejected) -> full re-login.
            evict(ACCESS_KEY);
            loginFresh();
        } catch (Exception e) {
            // Keep the current (still-valid) token; 401-recovery on send covers unexpected expiry.
            log.error("[DSDP] Scheduled refresh failed; keeping current token", e);
        }
    }

    /** @return true if the access token was successfully renewed via the refresh token. */
    private boolean refreshViaRefreshToken() throws IOException {
        String refreshToken = getFromRedis(REFRESH_KEY);
        if (TextUtils.isEmpty(refreshToken)) {
            return false;
        }
        Response<SafaricomRestOAuthResponse> res =
                safaricomRestInterface.refreshToken("Bearer " + refreshToken).execute();
        if (res.isSuccessful() && res.body() != null && !TextUtils.isEmpty(res.body().getToken())) {
            String newRefresh = TextUtils.isEmpty(res.body().getRefreshToken())
                    ? refreshToken : res.body().getRefreshToken();
            cacheTokens(res.body().getToken(), newRefresh);
            log.info("[DSDP] Access token refreshed via refresh token");
            return true;
        }
        log.warn("[DSDP] Refresh token rejected (HTTP {}), will fall back to full login", res.code());
        evict(REFRESH_KEY);
        return false;
    }

    /** Read a token from Redis. On any Redis error, return null so the caller fetches fresh. */
    private String getFromRedis(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("[DSDP] Redis unavailable reading {} — will fetch fresh: {}", key, e.getMessage());
            return null;
        }
    }

    /** Cache access + refresh tokens, each with a TTL derived from its own JWT {@code exp}. */
    private void cacheTokens(String accessToken, String refreshToken) {
        cache(ACCESS_KEY, accessToken);
        if (!TextUtils.isEmpty(refreshToken)) {
            cache(REFRESH_KEY, refreshToken);
        }
    }

    /** Store a token in Redis with a TTL of (JWT exp − 2 min), falling back to the configured default. */
    private void cache(String key, String token) {
        if (TextUtils.isEmpty(token)) return;
        try {
            redisTemplate.opsForValue().set(key, token, ttlFor(token));
        } catch (Exception e) {
            log.error("[DSDP] Failed to cache {} in Redis: {}", key, e.getMessage());
        }
    }

    private void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[DSDP] Could not evict {} from Redis: {}", key, e.getMessage());
        }
    }

    /**
     * TTL from the JWT {@code exp} claim minus a safety margin. The DSDP tokens are JWTs whose payload
     * carries {@code exp} (epoch seconds); if it can't be parsed we fall back to the configured default.
     */
    private Duration ttlFor(String jwt) {
        Long exp = parseExpEpochSeconds(jwt);
        if (exp != null) {
            Duration ttl = Duration.between(Instant.now(), Instant.ofEpochSecond(exp)).minus(EXPIRY_MARGIN);
            if (!ttl.isNegative() && !ttl.isZero()) {
                return ttl;
            }
        }
        return Duration.ofMinutes(Math.max(1, fallbackTtlMinutes - 2));
    }

    /** Extract the {@code exp} (epoch seconds) from a JWT payload, or null if it can't be read. */
    private Long parseExpEpochSeconds(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            int i = payload.indexOf("\"exp\"");
            if (i < 0) return null;
            int colon = payload.indexOf(':', i);
            if (colon < 0) return null;
            StringBuilder digits = new StringBuilder();
            for (int j = colon + 1; j < payload.length(); j++) {
                char c = payload.charAt(j);
                if (Character.isDigit(c)) digits.append(c);
                else if (digits.length() > 0) break;
            }
            return digits.length() > 0 ? Long.parseLong(digits.toString()) : null;
        } catch (Exception e) {
            log.warn("[DSDP] Could not parse exp from token: {}", e.getMessage());
            return null;
        }
    }
}
