package com.spa.smart_gate_springboot.menu;

import com.spa.smart_gate_springboot.dto.Layers;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(schema = "js_core", name = "menu")
public class Menu {
    @Id
    @GeneratedValue
    private UUID mnId;
    @Column(nullable = false)
    private String mnLink;

    private String mnName;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Layers mnOwner;
    private UUID mnParentId;
    private String mnIcons;

    @Transient
    @Builder.Default
    private List<Menu> children = new ArrayList<>();
}

