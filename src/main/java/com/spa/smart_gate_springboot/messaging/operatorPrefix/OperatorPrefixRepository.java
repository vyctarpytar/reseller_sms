package com.spa.smart_gate_springboot.messaging.operatorPrefix;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperatorPrefixRepository extends JpaRepository<OperatorPrefix, Long> {
    Optional<OperatorPrefix> findById(long id);

    OperatorPrefix findByOpPrefixAndOpOperator(long opPrefix, String opOperator);
}
