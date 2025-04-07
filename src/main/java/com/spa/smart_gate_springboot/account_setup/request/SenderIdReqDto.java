package com.spa.smart_gate_springboot.account_setup.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class SenderIdReqDto {

    private String telcos; // saf, airtel
    private String senderIdType;
    private String desc;
    private String instType;
    private String serviceOwnership;
    private String costCover;
    private String keyword;

    private String reKraFileName;
    private String reIncorporationCertFileName;
    private String reAuthorizationFileName;

}
