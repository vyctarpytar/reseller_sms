package com.spa.smart_gate_springboot.account_setup.request;

public enum ReStatus{
    PENDING,PROCESSING,PROCESSED,ACTIVE,DELETED;

    public static boolean exists(String status) {
        for (ReStatus reStatus : ReStatus.values()) {
            if (reStatus.name().equalsIgnoreCase(status)) {
                return true;
            }
        }
        return false;
    }
}
