package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface AirtelNumberRepository extends JpaRepository<AirtelNumber, Long> {

    boolean existsByAnNumber(String msgSubMobileNo);

    /**
     * Delete every row learned under the old rule (recorded on any Airtel send, which swept in
     * Safaricom numbers). Those are exactly the rows with no {@code an_reason}; rows recorded from a
     * {@code DeliveryImpossible} DLR always carry one, so this is safe to run on every boot.
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "delete from msg.airtel_numbers where an_reason is null")
    int deleteLegacyRows();
}
