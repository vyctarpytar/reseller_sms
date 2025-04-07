package com.spa.smart_gate_springboot.user;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto {
    private UUID usrId;
    private String firstname;
    private String lastname;
    private String email;
    private String phoneNumber; //do not update phone number
    private String usrNationalId;
    private UsrStatus usrStatus;
    private String usrLogo;
    private String layer;
    private UUID accId;
    private UUID resellerId;

    private Role role;

    private int start;
    private int limit;
    private String sortColumn;
}
