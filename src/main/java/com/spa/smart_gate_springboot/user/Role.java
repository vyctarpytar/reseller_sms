package com.spa.smart_gate_springboot.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Set;

import static com.spa.smart_gate_springboot.user.Permission.*;


@RequiredArgsConstructor
public enum Role {

    VIEWER(Collections.emptySet()), // view only

    SUPER_ADMIN(Set.of(
            SUPER_ADMIN_READ,
            SUPER_ADMIN_UPDATE,
            SUPER_ADMIN_CREATE,
            SUPER_ADMIN_DELETE
    )),
    ACCOUNTANT(Set.of(
            ENABLE_CLIENT,
            DISABLE_CLIENTS,
            LOAD_CREDIT,
            SALE_CREATE_CUSTOMER)),
    ADMIN(
            Set.of(
                    ADMIN_READ,
                    ADMIN_UPDATE,
                    ADMIN_DELETE,
                    ADMIN_CREATE,
                    CREATE_TEMPLATE,
                    SEND_SMS,
                    LOAD_CREDIT
            )
    ),
    CAMPAIGN_ADMIN(
            Set.of(
                    CREATE_TEMPLATE,
                    SEND_SMS
            )
    ),
    MANAGER(
            Set.of(
                    MANAGER_READ,
                    MANAGER_UPDATE,
                    MANAGER_DELETE,
                    MANAGER_CREATE,
                    MANAGER_APPROVE_CREDIT,
                    SALE_CREATE_CUSTOMER,
                    SALE_INTITATE_CREDIT,
                    SALE_APPROVE_ACCOUNT_REQUEST,
                    CREATE_TEMPLATE,
                    SEND_SMS, LOAD_CREDIT
            )
    ),
    SALE(
            Set.of(
                    SALE_CREATE_CUSTOMER,
                    SALE_INTITATE_CREDIT,
                    SALE_APPROVE_ACCOUNT_REQUEST
            )
    );

    @Getter
    private final Set<Permission> permissions;


}
