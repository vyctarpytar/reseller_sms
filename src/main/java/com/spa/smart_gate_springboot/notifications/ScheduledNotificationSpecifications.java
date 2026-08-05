package com.spa.smart_gate_springboot.notifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

// Specification rather than (:param IS NULL OR col = :param) JPQL, which breaks on Postgres
// with 42P18 (untyped bind parameter) whenever a filter is null.
public final class ScheduledNotificationSpecifications {

    private ScheduledNotificationSpecifications() {
    }

    public static Specification<ScheduledNotification> filter(String status, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("snStatus"), ScheduledNotification.STATUS_DELETED));

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("snStatus"), status.trim().toUpperCase()));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("snName")), like),
                        cb.like(cb.lower(root.get("snMessage")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
