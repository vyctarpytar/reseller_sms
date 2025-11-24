package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AirtelNumberRepository extends JpaRepository<AirtelNumber, Long> {

    boolean existsByAnNumber(@NotNull(message = "msgSubMobileNo cannot be null") String msgSubMobileNo);
}
