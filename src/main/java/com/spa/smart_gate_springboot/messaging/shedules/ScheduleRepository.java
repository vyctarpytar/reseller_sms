package com.spa.smart_gate_springboot.messaging.shedules;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    List<Schedule> findAllBySchReleaseTimeEqualsAndSchStatus(String schReleaseTime,String schStatus);
    List<Schedule> findAllBySchAccIdOrderBySchCreatedOn(UUID id);
}

