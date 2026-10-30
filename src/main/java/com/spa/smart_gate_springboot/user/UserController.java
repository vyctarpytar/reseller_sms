package com.spa.smart_gate_springboot.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.http.util.TextUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AccountService accountService;

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/get")
    public StandardJsonResponse getAllUsers(HttpServletRequest request , @RequestBody UserDto userFilter,
                                            @RequestParam(required = false) String reseller_id,
                                            @RequestParam(required = false) String account_id) {
        User user = userService.getCurrentUser(request);

        if(reseller_id != null) userFilter.setResellerId(UUID.fromString(reseller_id));
        // Drilled into an account: list that account's users only.
        UUID accId = accountService.resolveAccountScope(user, account_id);
        if (accId != null) userFilter.setAccId(accId);

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
    public StandardJsonResponse createUser(@RequestBody  UserDto userDto, HttpServletRequest request,
                                           @RequestParam(required = false) String reseller_id,
                                           @RequestParam(required = false) String account_id) {
        User authed = userService.getCurrentUser(request);
        return userService.saveUser(userDto, authed, resolveTargetScope(authed, reseller_id, account_id));
    }

    /**
     * The tenant a user created from this screen belongs to: the reseller/account the caller is
     * drilled into, not the caller's own. A TOP user drilled into an account creates an ACCOUNT
     * user of that account and its reseller. RESELLER users may only drill into their own accounts
     * (resolveAccountScope throws otherwise); ACCOUNT users are pinned to their own account.
     */
    private UserService.UserScope resolveTargetScope(User authed, String resellerId, String accountId) {
        if (authed.getLayer() == Layers.ACCOUNT) {
            return new UserService.UserScope(Layers.ACCOUNT, authed.getUsrResellerId(), authed.getUsrAccId());
        }
        UUID accId = accountService.resolveAccountScope(authed, accountId);
        if (accId != null) {
            Account acc = accountService.findByAccId(accId);
            return new UserService.UserScope(Layers.ACCOUNT, acc.getAccResellerId(), accId);
        }
        if (authed.getLayer() == Layers.RESELLER) {
            return new UserService.UserScope(Layers.RESELLER, authed.getUsrResellerId(), null);
        }
        if (!TextUtils.isEmpty(resellerId)) {
            return new UserService.UserScope(Layers.RESELLER, UUID.fromString(resellerId), null);
        }
        return new UserService.UserScope(Layers.TOP, null, null);
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
    public StandardJsonResponse getRoles(HttpServletRequest request,
                                         @RequestParam(required = false) String reseller_id,
                                         @RequestParam(required = false) String account_id) {
        var user = userService.getCurrentUser(request);
        return userService.getRoles(resolveTargetScope(user, reseller_id, account_id).layer());
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


