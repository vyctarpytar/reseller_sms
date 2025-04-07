package com.spa.smart_gate_springboot.ndovuPay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class Organisation {
        private String orgCounty;
        private String orgName;
        private String orgEmail;
        private String orgMobile;
        private String orgCertificateOfIncorporation;
        private String orgKraPin;
        private String orgLocation;
        private String orgTown;
        private String usrLastName;
        private String usrFirstName;
        private String usrEmail;
        private String usrMobileNumber;
        private String usrEncryptedPassword;


}
