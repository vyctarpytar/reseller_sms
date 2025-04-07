package com.spa.smart_gate_springboot.account_setup.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface MemberRepository extends JpaRepository<ChMember, UUID> {
//    List<ChMember> findBychGroupId(UUID grpId);
    List<ChMember> findByChGroupIdOrderByChIdDesc(UUID grpId);

    List<ChMember> findByChAccIdOrderByChIdDesc(UUID accId);
}
