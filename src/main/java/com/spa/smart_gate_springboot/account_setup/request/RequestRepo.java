package com.spa.smart_gate_springboot.account_setup.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestRepo extends JpaRepository<RequestEntity, UUID> {


    @Query("SELECT r FROM RequestEntity r WHERE (:reqStatus IS NULL OR r.reStatus = :reqStatus) AND r.reResellerId = :usrResellerId")
    List<RequestEntity> findByReStatusAndReResellerId(ReStatus reqStatus, @Param("usrResellerId") UUID usrResellerId);

    @Query("SELECT r FROM RequestEntity r WHERE :reqStatus IS NULL OR r.reStatus = :reqStatus")
    List<RequestEntity> findByRequestStatus(ReStatus reqStatus);

    Optional<RequestEntity> findByReSetUpId(UUID shId);
}

