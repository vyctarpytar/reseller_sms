package com.spa.smart_gate_springboot.user;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum UsrStatus{
    ACTIVE,INACTIVE,DELETED
}
