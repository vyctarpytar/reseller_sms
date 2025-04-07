package com.spa.smart_gate_springboot.messaging.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MsgDeliveryRepository extends JpaRepository<MsgDelivery, Long> {

    List<MsgDelivery> findByMsgdDelCode(String code);

    @Query(nativeQuery = true, value = """
select d.*
from msg.delivery d,
     msg.message_queue_arc
where msgd_msg_id = msg_id
  and msg_status != coalesce(d.msgd_status,'PENDING')
  and msg_status != 'DeliveredToTerminal'
and coalesce(d.msgd_status,'PENDING') != 'PENDING'
""")
    List<MsgDelivery> reconDeliveryNotes();
}
