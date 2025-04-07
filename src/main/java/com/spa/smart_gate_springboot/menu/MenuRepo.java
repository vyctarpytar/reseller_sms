package com.spa.smart_gate_springboot.menu;

import com.spa.smart_gate_springboot.dto.Layers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MenuRepo extends JpaRepository<Menu, UUID> {


    @Query(value = "WITH RECURSIVE MenuTree AS (" +
            "SELECT m.mn_id, m.mn_link, m.mn_name, m.mn_owner, m.mn_parent_id, m.mn_icons, 1 AS level " +
            "FROM js_core.menu m " +
            "WHERE m.mn_parent_id IS NULL AND m.mn_owner IN (:owner, 'ALL') " +
            "UNION ALL " +
            "SELECT m.mn_id, m.mn_link, m.mn_name, m.mn_owner, m.mn_parent_id, m.mn_icons, mt.level + 1 " +
            "FROM js_core.menu m " +
            "JOIN MenuTree mt ON m.mn_parent_id = mt.mn_id " +
            "WHERE m.mn_owner IN (:owner, 'ALL') " +
            ") " +
            "SELECT * FROM MenuTree",
            nativeQuery = true)
    List<Menu> findMenuTreeByOwner(@Param("owner") String owner);
}
