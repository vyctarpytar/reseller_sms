package com.spa.smart_gate_springboot.messaging.shedules;


import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/schedule")
@RequiredArgsConstructor
@Slf4j
public class ScheduleController{

    private final UserService userService;
    private final ScheduleService scheduleService;

    @PostMapping
    public StandardJsonResponse getSchedules(HttpServletRequest request, ScheduleFilterDto filterDto) {
        var user = userService.getCurrentUser(request);
        filterDto.setSchUsrId(user.getUsrId());
        filterDto.setSchAccId(user.getUsrAccId());
         return scheduleService.getFilteredSchedules(filterDto,user);
    }
}
