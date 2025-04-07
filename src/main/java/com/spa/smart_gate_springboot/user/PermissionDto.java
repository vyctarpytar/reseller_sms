package com.spa.smart_gate_springboot.user;

import lombok.*;

import java.util.Set;

@Builder
@AllArgsConstructor
@Data
public class PermissionDto{
    private String name;
    private Set<Permission> permission;
}
