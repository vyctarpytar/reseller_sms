package com.spa.smart_gate_springboot.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledNotificationRepository extends JpaRepository<ScheduledNotification, UUID>,
        JpaSpecificationExecutor<ScheduledNotification> {

    List<ScheduledNotification> findBySnStatusAndSnNextRunAtLessThanEqual(String snStatus, LocalDateTime now);

    boolean existsBySnSeedKey(String snSeedKey);
}
