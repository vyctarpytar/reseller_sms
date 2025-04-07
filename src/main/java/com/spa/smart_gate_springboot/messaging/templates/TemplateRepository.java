package com.spa.smart_gate_springboot.messaging.templates;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TemplateRepository extends JpaRepository<Template, UUID> {


    @Query(value = """
    SELECT * FROM msg.msg_templates
    WHERE  tmp_acc_by_id = cast(:tmpAccId as UUID)
      AND (:tmpMessage IS NULL OR tmp_message ILIKE :tmpMessage)
    """, nativeQuery = true)
    Page<Template> findFilteredMessageTemplate(@Param("tmpAccId") UUID tmpAccId, @Param("tmpMessage") String tmpMessage, Pageable pageable);


}

