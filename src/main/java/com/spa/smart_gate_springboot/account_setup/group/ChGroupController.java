package com.spa.smart_gate_springboot.account_setup.group;

import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/groups")
@RequiredArgsConstructor
public class ChGroupController {

    private final UserService userService;
    private final ChGroupService chGroupService;



    @GetMapping("/{id}")
    public StandardJsonResponse getGroupById(@PathVariable UUID id) {
        return chGroupService.findById(id);

    }

    @GetMapping
    public StandardJsonResponse getGroupByAccId(HttpServletRequest request) {
        User user = userService.getCurrentUser(request);
        return chGroupService.findByAccountId(user.getUsrAccId());

    }

    @PostMapping
    public StandardJsonResponse createGroup(@RequestBody ChGroup chGroup,HttpServletRequest request) {
        User user = userService.getCurrentUser(request);
        return chGroupService.createGroup(chGroup,user);
    }


    @DeleteMapping("/{id}")
    public StandardJsonResponse deleteGroup(@PathVariable UUID id) {
        return chGroupService.deleteById(id);
    }
}

