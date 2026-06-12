package com.spa.smart_gate_springboot.payment.mpesa.b2c;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface B2cTransactionRepository extends JpaRepository<B2cTransaction, UUID> {

    List<B2cTransaction> findByStatus(B2cTransactionStatus status);

    Optional<B2cTransaction> findByConversationId(String conversationId);

    boolean existsByExternalRef(String externalRef);

    @Query("""
            SELECT t FROM B2cTransaction t
            WHERE (:walletCode IS NULL OR t.walletCode = :walletCode)
              AND (:status IS NULL OR t.status = :status)
              AND (:from IS NULL OR t.createdAt >= :from)
              AND (:to IS NULL OR t.createdAt <= :to)
            ORDER BY t.createdAt DESC
            """)
    Page<B2cTransaction> findFiltered(@Param("walletCode") String walletCode,
                                      @Param("status") B2cTransactionStatus status,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      Pageable pageable);
}
