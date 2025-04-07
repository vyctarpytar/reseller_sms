package com.spa.smart_gate_springboot.dashboad;

import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.messaging.send_message.dtos.FilterDto;
import com.spa.smart_gate_springboot.user.Role;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/dash")
@RequiredArgsConstructor
public class DashBoardController {
    private final DashBoardService dashBoardService;
    private final UserService userService;

    @PostMapping
    public StandardJsonResponse getMainDashBoard(HttpServletRequest request, @RequestBody FilterDto filterDto) {
        User user = userService.getCurrentUser(request);
        if (user.getLayer().equals(Layers.ACCOUNT)) {
            filterDto.setMsgAccId(user.getUsrAccId());
        } else if (user.getRole().equals(Role.SALE)) {
            // show dashboard for sales user
            filterDto.setMsgSaleUserId(user.getUsrId());
        } else if (user.getLayer().equals(Layers.RESELLER)) {
            filterDto.setMsgResellerId(user.getUsrResellerId());
        }

        if (user.getLayer().equals(Layers.TOP)) {
            if (user.getUsrId().equals(UUID.fromString("50b0ad9d-7471-4143-8f4b-57838360cb4a"))) { // top synq-Africa
                filterDto.setMsgResellerId(UUID.fromString("c3a1822b-72f3-4176-9b64-093fbf0a8c0d")); // show synq tel
            }
        }
        return dashBoardService.getMainDashboard(filterDto);
    }
}
