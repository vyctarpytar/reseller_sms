package com.spa.smart_gate_springboot.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/get")
    public StandardJsonResponse getAllUsers(HttpServletRequest request , @RequestBody UserDto userFilter,
                                            @RequestParam(required = false) String reseller_id) {
        User user = userService.getCurrentUser(request);

        if(reseller_id != null) userFilter.setResellerId(UUID.fromString(reseller_id));

        return userService.getAllUsers(user, userFilter);
    }

    @GetMapping("me")
    public StandardJsonResponse getUserById(HttpServletRequest request) {
        StandardJsonResponse response = new StandardJsonResponse();
        User user = userService.getCurrentUser(request);
        response.setData("response", List.of(user), response);
        return response;
    }

    @PostMapping
    public StandardJsonResponse createUser(@RequestBody  UserDto userDto, HttpServletRequest request) {
        User authed = userService.getCurrentUser(request);
        return userService.saveUser(userDto, authed);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/{userId}/permissions")
    public StandardJsonResponse assignPermissions(@PathVariable UUID userId, @RequestBody String permissionJson) {
        StandardJsonResponse resp = new StandardJsonResponse();
        try {
            System.out.println("the pers:" + permissionJson);
            // Parse the JSON input
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(permissionJson);
            JsonNode permissionValuesNode = rootNode.path("permissionValue");

            // Convert the JSON array to a Set of permission strings
            Set<String> permissionSet = new HashSet<>();
            if (permissionValuesNode.isArray()) {
                for (JsonNode permissionNode : permissionValuesNode) {
                    String permissionValue = permissionNode.asText();
                    permissionSet.add(permissionValue);
                }
            }
            Set<Permission> permissions = new HashSet<>();
            for (String permissionValue : permissionSet) {
                try {
                    Permission permission = Permission.fromString(permissionValue);
                    permissions.add(permission);

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    resp.setSuccess(false);
                    resp.setMessage("message", "Invalid permission value: " + e.getLocalizedMessage(), resp);
                    return resp;
                }
            }
            // Assign permissions to the user
            return userService.assignPermissionsToUser(userId, permissions);

        } catch (Exception e) {
            e.printStackTrace();
            resp.setSuccess(false);
            resp.setMessage("message", "An error occurred: " + e.getLocalizedMessage(), resp);
            return resp;
        }
    }


    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("permission/{roleName}")
    public StandardJsonResponse getPermissions(@PathVariable String roleName) {
        StandardJsonResponse resp = new StandardJsonResponse();
        List<PermissionDto> permissionDtos = Arrays.stream(Role.values())
                .filter(f -> f.name().equalsIgnoreCase(roleName))
                .map(role ->PermissionDto.builder().name(role.name()).permission(role.getPermissions()).build())
                .collect(Collectors.toList());
        resp.setTotal(permissionDtos.size());
        resp.setData("result", permissionDtos, resp);
        return resp;
    }


    @PostMapping("/new-email-otp/{id}")
    public StandardJsonResponse newEmailOtp(@PathVariable UUID id, @RequestParam(required = true) String newEmail) {
        return userService.newEmailOtp(id, newEmail);
    }



    @PostMapping("/update-password")
    public StandardJsonResponse updatePasssword(@RequestBody UserSecret userSecret, HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        DraftSecret draftSecret = DraftSecret.builder().usrSecret(userSecret.getUserSecret()).usrId(user.getUsrId()).build();
        return userService.updatePasssword(draftSecret);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("roles")
    public StandardJsonResponse getRoles(HttpServletRequest request) {
        var user = userService.getCurrentUser(request);
        return userService.getRoles(user);
    }


    @GetMapping("distinct-status")
    public StandardJsonResponse setDistinctUsrStatus() {
        StandardJsonResponse resp = new StandardJsonResponse();
        List<StatusDto> statusDto = Arrays.stream(UsrStatus.values())
                .map(i -> StatusDto.builder().usrStatus(i.name()).build())
                .collect(Collectors.toList());
        resp.setTotal(statusDto.size());
        resp.setData("result", statusDto, resp);
        return resp;
    }


}


