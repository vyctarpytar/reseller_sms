package com.spa.smart_gate_springboot.notifications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScheduledNotificationLogRepository extends JpaRepository<ScheduledNotificationLog, UUID> {

    Page<ScheduledNotificationLog> findBySnlNotificationIdOrderBySnlRunAtDesc(UUID snlNotificationId, Pageable pageable);
}
