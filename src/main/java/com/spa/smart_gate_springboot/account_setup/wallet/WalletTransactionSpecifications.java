package com.spa.smart_gate_springboot.account_setup.wallet;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dynamic, hierarchy-aware filters for the {@link WalletTransaction} statement ledger.
 *
 * <p>Each criterion is added only when its value is non-null, so an absent filter contributes no
 * predicate at all (platform-wide for TOP). This deliberately avoids the
 * {@code (:param IS NULL OR col = :param)} JPQL idiom: a bind parameter standing alone in
 * {@code :param IS NULL} carries no type, which breaks on PostgreSQL with
 * {@code 42P18 "could not determine data type of parameter $N"} whenever the filter is null.
 * Newest-first ordering is supplied by the caller via the {@code Pageable}'s {@code Sort}.
 */
public final class WalletTransactionSpecifications {

    private WalletTransactionSpecifications() {
    }

    public static Specification<WalletTransaction> filter(UUID resellerId,
                                                          UUID accountId,
                                                          WalletValueType valueType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (resellerId != null) {
                predicates.add(cb.equal(root.get("resellerId"), resellerId));
            }
            if (accountId != null) {
                predicates.add(cb.equal(root.get("accountId"), accountId));
            }
            if (valueType != null) {
                predicates.add(cb.equal(root.get("valueType"), valueType));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
