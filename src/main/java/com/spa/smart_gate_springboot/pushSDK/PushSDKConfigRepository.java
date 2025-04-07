package com.spa.smart_gate_springboot.pushSDK;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PushSDKConfigRepository extends JpaRepository<PushSDKConfig, UUID> {
    Optional<PushSDKConfig> findByMpShortCode(String shortCode);
}
