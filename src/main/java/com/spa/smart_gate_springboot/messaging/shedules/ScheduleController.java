package com.spa.smart_gate_springboot.messaging.shedules;


import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/schedule")
@RequiredArgsConstructor
@Slf4j
public class ScheduleController{

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final AccountService accountService;

    @PostMapping
    public StandardJsonResponse getSchedules(HttpServletRequest request, ScheduleFilterDto filterDto, @RequestParam(required = false) String account_id) {
        var user = userService.getCurrentUser(request);
        filterDto.setSchUsrId(user.getUsrId());
        filterDto.setSchAccId(user.getUsrAccId());
        // Drill-down: scope schedules to the single account being viewed (ownership enforced).
        UUID accScope = accountService.resolveAccountScope(user, account_id);
        if (accScope != null) filterDto.setSchAccId(accScope);
         return scheduleService.getFilteredSchedules(filterDto,user);
    }

    @PostMapping("/update")
    public StandardJsonResponse updateSchedule(HttpServletRequest request, @RequestBody ScheduleDto scheduledto) {
        var user = userService.getCurrentUser(request);
        return scheduleService.updateSchedule(scheduledto,user);
    }
    @DeleteMapping("/delete/{scheduleId}")
    public StandardJsonResponse deleteSchedule(HttpServletRequest request, @PathVariable UUID scheduleId) {
        var user = userService.getCurrentUser(request);

        return scheduleService.disaleSchedule(scheduleId,user);
    }
}
