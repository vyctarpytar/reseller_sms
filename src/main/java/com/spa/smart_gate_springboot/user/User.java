package com.spa.smart_gate_springboot.user;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.spa.smart_gate_springboot.dto.Layers;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stp_user", schema = "js_core")
public class User implements UserDetails {

    @Id
    @GeneratedValue
    private UUID usrId;
    private String firstname;
    private String lastname;
    @Column(nullable = false, unique = true)
    private String email;
    @NotNull
    @Column(updatable = false)
    private String phoneNumber;
    private String usrNationalId;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Status Cannot be Null")
    private UsrStatus usrStatus;
    private String usrOtpStatus;

    private String password;
    private String usrLogo;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false, nullable = false)
    private Layers layer;

    //todo uncomment below
//    @NotNull
    private UUID brnId;
    //    @NotNull
    private UUID usrResellerId;
    private UUID usrAccId;

    @JsonIgnore
    private String usrEmailOtp;
    @JsonIgnore
    private String usrPhoneOtp;

    @JsonIgnore
    private String usrPhoneWithdrawOtp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Role Cannot be Null")
    private Role role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"), schema = "js_core")
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Set<Permission> permissions = new HashSet<>();
    private LocalDateTime createdDate;
    private UUID createdBy;

    private Boolean usrChangePassword;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = permissions.stream().map(permission -> new SimpleGrantedAuthority(permission.getPermission())).collect(Collectors.toSet());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

