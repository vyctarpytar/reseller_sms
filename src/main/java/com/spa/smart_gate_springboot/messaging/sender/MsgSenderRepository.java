package com.spa.smart_gate_springboot.messaging.sender;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MsgSenderRepository extends JpaRepository<MsgSender, Long> {

}
