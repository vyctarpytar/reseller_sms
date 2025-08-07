package com.spa.smart_gate_springboot.menu;


import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.Role;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {
    private final MenuRepo menuRepo;
    private final UserService userService;



//    @PostConstruct
    private void init() {
        menuRepo.deleteAll();
        Menu m1 = Menu.builder().mnLink("dashboard").mnName("dashboard").mnOwner(Layers.TOP).build();
        Menu m2 = Menu.builder().mnLink("dashboard-reseller").mnName("dashboard").mnOwner(Layers.RESELLER).build();
        Menu m3 = Menu.builder().mnLink("dashboard-account").mnName("dashboard").mnOwner(Layers.ACCOUNT).build();

        Menu m14 = Menu.builder().mnLink("billing").mnName("Billing").mnOwner(Layers.RESELLER).mnIcons("adminAccountSvg").build();



        Menu m4 = Menu.builder().mnLink("bulk-sms").mnName("bulksms").mnOwner(Layers.ACCOUNT).mnIcons("smsSvg").build();
        Menu m5 = Menu.builder().mnLink("users").mnName("Users").mnOwner(Layers.ALL).mnIcons("usersSvg").build();
        Menu m6 = Menu.builder().mnLink("resellers-list").mnName("Resellers").mnOwner(Layers.TOP).mnIcons("resellerSvg").build();
        Menu m7 = Menu.builder().mnLink("product-request-list").mnName("Process Request").mnOwner(Layers.TOP).mnIcons("requestSvg").build();

        Menu m9 = Menu.builder().mnLink("account-list").mnName("Account").mnOwner(Layers.RESELLER).mnIcons("accountSvg").build();

        Menu m10 = Menu.builder().mnLink("credit").mnName("Credits Statement").mnOwner(Layers.TOP).mnIcons("creditSvg").build();
        Menu m11 = Menu.builder().mnLink("credit").mnName("Credits Statement").mnOwner(Layers.RESELLER).mnIcons("creditSvg").build();

        Menu m13 = Menu.builder().mnLink("sms-request-list").mnName("External Requests").mnOwner(Layers.ACCOUNT).mnIcons("externalSvg").build();

        Menu m15 = Menu.builder().mnLink("sent-sms").mnName("Sent SMS").mnOwner(Layers.RESELLER).mnIcons("smsSvg").build();
        Menu m16 = Menu.builder().mnLink("sent-sms").mnName("Sent SMS").mnOwner(Layers.TOP).mnIcons("smsSvg").build();


        Menu m17 = Menu.builder().mnLink("sender-requests").mnName("Sender Requests").mnOwner(Layers.RESELLER).mnIcons("requestSvg").build();


        Menu m18 = Menu.builder().mnLink("report").mnName("reports").mnOwner(Layers.ALL).build();
        Menu m19 = Menu.builder().mnLink("dev-sandbox").mnName("Dev SandBox").mnOwner(Layers.ACCOUNT).mnIcons("requestSvg").build();




        menuRepo.saveAllAndFlush(List.of(m1, m2, m3, m14, m4, m5, m6, m7,  m9, m10, m11,  m13,m15,m16, m17,m18,m19));

//        Menu m4a = Menu.builder().mnLink("sms-request-list").mnName("request").mnOwner(Layers.ACCOUNT).mnParentId(m4.getMnId()).build();
//        Menu m4b = Menu.builder().mnLink("inbox").mnName("inbox").mnOwner(Layers.ACCOUNT).mnParentId(m4.getMnId()).build();
//        Menu m4c = Menu.builder().mnLink("bulk-sms").mnName("bulksms").mnOwner(Layers.ACCOUNT).mnParentId(m4.getMnId()).build();

//        menuRepo.saveAllAndFlush(List.of(m4c));


        Menu m4c1 = Menu.builder().mnLink("outbox").mnName("Create SMS").mnOwner(Layers.ACCOUNT).mnParentId(m4.getMnId()).build();
        Menu m4c2 = Menu.builder().mnLink("credit").mnName("Credits Statement").mnOwner(Layers.ACCOUNT).mnParentId(m4.getMnId()).mnIcons("creditSvg").build();
        Menu m4c3 = Menu.builder().mnLink("sent-sms").mnName("Sent SMS").mnOwner(Layers.ACCOUNT).mnParentId(m4.getMnId()).build();
        Menu m4c6 = Menu.builder().mnLink("group").mnName("Group SMS").mnOwner(Layers.ACCOUNT).mnParentId(m4.getMnId()).build();
        Menu m4c4 = Menu.builder().mnLink("invoices").mnName("invoices").mnOwner(Layers.ACCOUNT).mnParentId(m4.getMnId()).build();
        Menu m4c5 = Menu.builder().mnLink("templates").mnName("Templates").mnOwner(Layers.ACCOUNT).mnParentId(m4.getMnId()).build();
        Menu m4c7 = Menu.builder().mnLink("scheduled-sms").mnName("Scheduled Sms").mnOwner(Layers.ACCOUNT).mnParentId(m4.getMnId()).build();

        Menu m14a = Menu.builder().mnLink("account-admin").mnName("Manage Clients").mnOwner(Layers.RESELLER).mnParentId(m14.getMnId()).build();
        Menu m14b = Menu.builder().mnLink("payments").mnName("Payments").mnOwner(Layers.RESELLER).mnParentId(m14.getMnId()).build();
        Menu m14c = Menu.builder().mnLink("invoices").mnName("invoices").mnOwner(Layers.RESELLER).mnParentId(m14.getMnId()).build();
        Menu m14d = Menu.builder().mnLink("withdrawal").mnName("Wallet").mnOwner(Layers.RESELLER).mnIcons("withdrawalSvg").build();
        Menu m14e = Menu.builder().mnLink("withdrawal").mnName("Wallet").mnOwner(Layers.TOP).mnIcons("withdrawalSvg").build();

        Menu m17_a = Menu.builder().mnLink("sender-id-list").mnName("Map Sender Ids").mnOwner(Layers.RESELLER).mnParentId(m17.getMnId()).build();
        Menu m17_b = Menu.builder().mnLink("sms-request-list").mnName("External Requests").mnOwner(Layers.RESELLER).mnParentId(m17.getMnId()).build();
        Menu m17_c = Menu.builder().mnLink("code-request-list").mnName("Map Request").mnOwner(Layers.RESELLER).mnParentId(m17.getMnId()).build();

        Menu m18_a = Menu.builder().mnLink("status-analysis").mnName("Sms By Status Summary").mnOwner(Layers.ALL).mnParentId(m18.getMnId()).build();
        Menu m18_b = Menu.builder().mnLink("date-analysis").mnName("Sms By Date Summary").mnOwner(Layers.ALL).mnParentId(m18.getMnId()).build();

        menuRepo.saveAllAndFlush(List.of(m4c4, m4c2, m4c3,m4c6,m4c5,m4c7, m4c1, m14a,m14c,m14b,m14d,m14e, m17_a, m17_b, m17_c,m18_a,m18_b));
    }


    public List<Menu> getMenuTree(String layer,Role role ) {
        // todo come and order

        List<Menu> flatMenuList = menuRepo.findMenuTreeByOwner(layer);

        // if not SPA REMOVE Link account-admin
        if ( role != null && !role.equals(Role.ACCOUNTANT)) {
            flatMenuList = flatMenuList.stream().filter(menu ->!( menu.getMnLink().equalsIgnoreCase("billing"))).collect(Collectors.toList());
        }

        return buildTree(flatMenuList);
    }

    private List<Menu> buildTree(List<Menu> flatMenuList) {
        Map<UUID, Menu> menuMap = new HashMap<>();
        List<Menu> rootMenus = new ArrayList<>();

        for (Menu menu : flatMenuList) {
            menuMap.put(menu.getMnId(), menu);
        }

        for (Menu menu : flatMenuList) {
            if (menu.getMnParentId() == null) {
                rootMenus.add(menu);
            } else {
                Menu parent = menuMap.get(menu.getMnParentId());
                if (parent != null) {
                    parent.getChildren().add(menu);
                }
            }
        }
        return rootMenus;
    }

    public StandardJsonResponse findMenuByLayer(HttpServletRequest request, String resellerId, String accountId) {
        var resp = new StandardJsonResponse();

        String layer = userService.getCurrentUser(request).getLayer().name();
        Role role = userService.getCurrentUser(request).getRole();

        if(!TextUtils.isEmpty(resellerId)) {
            layer = Layers.RESELLER.name();
            role = null;
        }
        if(!TextUtils.isEmpty(accountId)) {
            layer = Layers.ACCOUNT.name();
            role = null;
        }

        log.info("Fetch Menu for Layer : {}  and Role : {}", layer, role);

        List<Menu> menuList = getMenuTree(layer, role);
        resp.setData("result", menuList, resp);
        resp.setMessage("bla", "Ok", resp);
        resp.setTotal(menuList.size());
        return resp;
    }
}
