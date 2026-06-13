package com.spa.smart_gate_springboot.payment.mpesa.b2c;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic filters for {@link B2cTransaction} payout history.
 *
 * <p>Each criterion is added only when its value is non-null, so absent filters contribute no
 * predicate at all. This deliberately avoids the {@code (:param IS NULL OR col = :param)} JPQL
 * idiom: a bind parameter standing alone in {@code :param IS NULL} carries no type, which breaks
 * on PostgreSQL with {@code 42P18 "could not determine data type of parameter $N"} whenever the
 * filter is null. Build dynamic filters this way rather than with the IS-NULL-OR pattern.
 */
public final class B2cTransactionSpecifications {

    private B2cTransactionSpecifications() {
    }

    public static Specification<B2cTransaction> filter(String walletCode,
                                                       B2cTransactionStatus status,
                                                       LocalDateTime from,
                                                       LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (walletCode != null) {
                predicates.add(cb.equal(root.get("walletCode"), walletCode));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("createdAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
