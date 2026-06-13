package com.spa.smart_gate_springboot.payment.mpesa.b2c;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface B2cTransactionRepository extends JpaRepository<B2cTransaction, UUID>,
        JpaSpecificationExecutor<B2cTransaction> {

    List<B2cTransaction> findByStatus(B2cTransactionStatus status);

    Optional<B2cTransaction> findByConversationId(String conversationId);

    boolean existsByExternalRef(String externalRef);

    // Filtered payout history is built dynamically via B2cTransactionSpecifications.filter(...)
    // and findAll(Specification, Pageable) from JpaSpecificationExecutor — null filters add no
    // predicate. We avoid the "(:param IS NULL OR col = :param)" JPQL pattern because a bare bind
    // param in ":param IS NULL" is untyped and breaks on PostgreSQL (42P18) when the filter is null.
}
