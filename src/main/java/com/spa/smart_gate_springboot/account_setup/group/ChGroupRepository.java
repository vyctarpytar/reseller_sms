package com.spa.smart_gate_springboot.account_setup.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChGroupRepository extends JpaRepository<ChGroup, UUID> {
    List<ChGroup> findByGroupAccId(UUID accId);
}
