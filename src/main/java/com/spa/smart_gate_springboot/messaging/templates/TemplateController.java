package com.spa.smart_gate_springboot.messaging.templates;

import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/msgTemp")
@RequiredArgsConstructor
public class TemplateController {
    private final TemplateService templateService;
    private final UserService userService;

    @PostMapping("list")
    public StandardJsonResponse fetchTemplate(HttpServletRequest request, @RequestBody TempFilterDto filterDto) {
        var user = userService.getCurrentUser(request);
        filterDto.setTmpAccId(user.getUsrAccId());
        return templateService.getMsgTemplate(filterDto);
    }

    @PostMapping("save")
    @PreAuthorize("hasAnyRole('CAMPAIGN_ADMIN','ADMIN')")
    public StandardJsonResponse saveNewTemplate(HttpServletRequest request, @RequestBody TempFilterDto filterDto) {
        var user = userService.getCurrentUser(request);
        filterDto.setTmpAccId(user.getUsrAccId());
        return templateService.saveMsgTemplate(filterDto, user);
    }

    @PostMapping("/update/{tmpId}")
    @PreAuthorize("hasAnyRole('CAMPAIGN_ADMIN','ADMIN')")
    public StandardJsonResponse updateTemplate(HttpServletRequest request, @RequestBody TempFilterDto filterDto, @PathVariable UUID tmpId) {
        var user = userService.getCurrentUser(request);
        filterDto.setTmpAccId(user.getUsrAccId());
        return templateService.updateMsgTemplate(tmpId, filterDto, user);
    }
}


