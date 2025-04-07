package com.spa.smart_gate_springboot.messaging.send_message.api;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByApiKey(String apiKey);
    Optional<ApiKey> findByApiAccIdAndActiveIsTrue (UUID apiAccId);
    Optional<ApiKey> findByApiAccId (UUID apiAccId);

    @Query(value = "SELECT COUNT(*) > 0 FROM js_core.api_key " +
            "WHERE api_key = :apiKey " +
            "AND active = true " +
            "AND (expiration_date IS NULL OR cast(expiration_date as date) > current_date)",
            nativeQuery = true)
    boolean existsValidApiKey(@Param("apiKey") String apiKey);
}