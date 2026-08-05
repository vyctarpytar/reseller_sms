package com.spa.smart_gate_springboot.notifications;

import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/notifications")
@RequiredArgsConstructor
public class ScheduledNotificationController {

    private final ScheduledNotificationService notificationService;
    private final UserService userService;

    @PostMapping("/list")
    public StandardJsonResponse list(HttpServletRequest request, @RequestBody NotificationFilterDto dto) {
        User user = userService.getCurrentUser(request);
        StandardJsonResponse denied = topGate(user);
        return denied != null ? denied : notificationService.list(dto);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/save")
    public StandardJsonResponse save(HttpServletRequest request, @RequestBody NotificationDto dto) {
        User user = userService.getCurrentUser(request);
        StandardJsonResponse denied = topGate(user);
        return denied != null ? denied : notificationService.save(dto, user);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/update/{snId}")
    public StandardJsonResponse update(HttpServletRequest request, @PathVariable UUID snId,
                                       @RequestBody NotificationDto dto) {
        User user = userService.getCurrentUser(request);
        StandardJsonResponse denied = topGate(user);
        return denied != null ? denied : notificationService.update(snId, dto, user);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/toggle/{snId}")
    public StandardJsonResponse toggle(HttpServletRequest request, @PathVariable UUID snId) {
        User user = userService.getCurrentUser(request);
        StandardJsonResponse denied = topGate(user);
        return denied != null ? denied : notificationService.toggle(snId, user);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/run/{snId}")
    public StandardJsonResponse run(HttpServletRequest request, @PathVariable UUID snId) {
        User user = userService.getCurrentUser(request);
        StandardJsonResponse denied = topGate(user);
        return denied != null ? denied : notificationService.runNow(snId, user);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @DeleteMapping("/delete/{snId}")
    public StandardJsonResponse delete(HttpServletRequest request, @PathVariable UUID snId) {
        User user = userService.getCurrentUser(request);
        StandardJsonResponse denied = topGate(user);
        return denied != null ? denied : notificationService.delete(snId, user);
    }

    @GetMapping("/logs/{snId}")
    public StandardJsonResponse logs(HttpServletRequest request, @PathVariable UUID snId,
                                     @RequestParam(defaultValue = "0") int start,
                                     @RequestParam(defaultValue = "10") int limit) {
        User user = userService.getCurrentUser(request);
        StandardJsonResponse denied = topGate(user);
        return denied != null ? denied : notificationService.logs(snId, start, limit);
    }

    @GetMapping("/frequencies")
    public StandardJsonResponse frequencies(HttpServletRequest request) {
        User user = userService.getCurrentUser(request);
        StandardJsonResponse denied = topGate(user);
        if (denied != null) {
            return denied;
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (NotificationFrequency frequency : NotificationFrequency.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("value", frequency.name());
            row.put("label", frequency.getLabel());
            rows.add(row);
        }

        StandardJsonResponse resp = new StandardJsonResponse();
        resp.setData("result", rows, resp);
        resp.setTotal(rows.size());
        return resp;
    }

    // Returns null when the caller is TOP; otherwise the 403 body to return as-is.
    private StandardJsonResponse topGate(User user) {
        if (user != null && user.getLayer() == Layers.TOP) {
            return null;
        }
        StandardJsonResponse forbidden = new StandardJsonResponse();
        forbidden.setSuccess(false);
        forbidden.setStatus(HttpStatus.FORBIDDEN.value());
        forbidden.setMessage("message", "Only TOP users can manage scheduled notifications", forbidden);
        return forbidden;
    }
}
