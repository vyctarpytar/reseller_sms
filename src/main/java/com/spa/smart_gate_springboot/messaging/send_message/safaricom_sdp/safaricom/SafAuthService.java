package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom;


import com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.models.SafAuthReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class SafAuthService {

    /** Redis key holding the current Safaricom SDP (v1) access token (shared across instances/restarts).
     *  Version-namespaced so it never collides with the v2 DSDP keys (safaricom:v2:dsdp:*). */
    private static final String REDIS_KEY = "safaricom:v1:sdp:access_token";

    private final SafaricomProperties safaricomProperties;
    private final SafaricomInterface safaricomInterface;
    private final StringRedisTemplate redisTemplate;

    @Value("${safaricom.api.version:v1}")
    private String safaricomApiVersion;

    /** Assumed token validity (the SDP token carries no expires_in). Cached for this minus 2 minutes. */
    @Value("${safaricom.token.expiry-minutes:60}")
    private int tokenExpiryMinutes;

    /**
     * Fetch a fresh token from Safaricom and cache it in Redis. {@code synchronized} + a re-check of
     * the cache makes concurrent misses collapse into a single login instead of stampeding Safaricom.
     */
    public synchronized String getTokenFromSafaricom() throws IOException {
        String cached = getTokenFromRedis();
        if (!TextUtils.isEmpty(cached)) {
            return cached; // another thread refreshed while we waited for the lock
        }
        

        SafAuthReq safAuthReq = SafAuthReq.builder()
                .password(safaricomProperties.getSafPassword())
                .username(safaricomProperties.getSafApiUserName())
                .build();

        Call<SafTokenResponse> call = safaricomInterface.getToken(
                String.valueOf(MediaType.APPLICATION_JSON), "XMLHttpRequest", safAuthReq);
        Response<SafTokenResponse> res = call.execute();

        if (res.isSuccessful()) {
            assert res.body() != null;
            String token = res.body().getToken();
            cacheToken(token);
            return token;
        }
        throw new RuntimeException("Safaricom Error : could not obtain token");
    }

    /** Hot path: return the cached token if present, otherwise fetch a fresh one (single-flight). */
    public String getAccessToken() throws IOException {
        String token = getTokenFromRedis();
        return TextUtils.isEmpty(token) ? getTokenFromSafaricom() : token;
    }

    /** Read the token from Redis. On any Redis error, return null so the caller fetches fresh. */
    public String getTokenFromRedis() {
        try {
            return redisTemplate.opsForValue().get(REDIS_KEY);
        } catch (Exception e) {
            log.error("Redis unavailable reading Safaricom token — will fetch fresh: {}", e.getMessage());
            return null;
        }
    }

    /** Store the token in Redis with a TTL of (configured expiry − 2 minutes). Never fails the send. */
    private void cacheToken(String token) {
        if (TextUtils.isEmpty(token)) return;
        try {
            Duration ttl = Duration.ofMinutes(Math.max(1, tokenExpiryMinutes - 2));
            redisTemplate.opsForValue().set(REDIS_KEY, token, ttl);
        } catch (Exception e) {
            log.error("Failed to cache Safaricom token in Redis: {}", e.getMessage());
        }
    }

    /** Proactively rotate the token before it expires: evict the cache, then re-fetch. */
    @Scheduled(cron = "0 */50 * * * *")
    public void refreshToken() {
        // Only pre-warm the SDP (v1) token when v1 is the selected provider; on v2 this would
        // otherwise hammer the legacy SDP login endpoint every 50 min for a token we never use.
        if (!"v1".equalsIgnoreCase(safaricomApiVersion)) {
            return;
        }
        log.info("Refreshing Safaricom token");
        try {
            try {
                redisTemplate.delete(REDIS_KEY);
            } catch (Exception e) {
                log.warn("Could not evict Safaricom token from Redis: {}", e.getMessage());
            }
            getTokenFromSafaricom();
        } catch (Exception e) {
            log.error("Error refreshing Safaricom access token", e);
        }
    }
}
