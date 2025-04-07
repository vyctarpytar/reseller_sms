package com.spa.smart_gate_springboot.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@AllArgsConstructor
public enum Permission {


    SUPER_ADMIN_READ("super_admin:read"),
    SUPER_ADMIN_UPDATE("super_admin:update"),
    SUPER_ADMIN_CREATE("super_admin:create"),
    SUPER_ADMIN_DELETE("super_admin:delete"),

    ENABLE_CLIENT("accountant:enable"),
    DISABLE_CLIENTS("accountant:disable"),
    LOAD_CREDIT("accountant:load_credit"),


    ADMIN_READ("admin:read"),
    ADMIN_UPDATE("admin:update"),
    ADMIN_CREATE("admin:create"),
    ADMIN_DELETE("admin:delete"),

    MANAGER_READ("management:read"),
    MANAGER_UPDATE("management:update"),
    MANAGER_CREATE("management:create"),
    MANAGER_DELETE("management:delete"),
    MANAGER_APPROVE_CREDIT("management:approve_credit"),

    SALE_CREATE_CUSTOMER("sale:create_customer"),
    SALE_INTITATE_CREDIT("sale:initiate_credit"),
    SALE_APPROVE_ACCOUNT_REQUEST("sale:approve_account_request"),


    CREATE_TEMPLATE("campaign_admin:create_template"),
    SEND_SMS("campaign_admin:send_template");

    @Getter
    private final String permission;

    // Optional method to convert from string value to enum
    public static Permission fromString(String text) {
        for (Permission permission : Permission.values()) {
            if (permission.name().equalsIgnoreCase(text)) {
                return permission;
            }
        }
        throw new IllegalArgumentException("No constant with permission " + text + " found");
    }

}
