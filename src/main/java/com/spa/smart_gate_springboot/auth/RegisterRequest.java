package com.spa.smart_gate_springboot.auth;


import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.Permission;
import com.spa.smart_gate_springboot.user.Role;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NonNull
    private String uuid;
    @NonNull
    private String username;
    @NonNull
    private String email;
    private Role role;
    @NonNull
    private String phonenumber;
    @NonNull
    private String password;
    private UUID brnid;
    private UUID instId;
    private Layers layers;

    private String firstname;
    private String lastname;
    private String usrNationalId;
    private Set<Permission> permissions ;
}

