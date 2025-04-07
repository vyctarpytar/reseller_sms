package com.spa.smart_gate_springboot.auth;

import com.spa.smart_gate_springboot.user.DraftSecret;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @PostMapping("/refresh-token")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        service.refreshToken(request, response);
    }


    @PatchMapping("/forgot-password")
    public StandardJsonResponse changePassword(@RequestBody PasswrdReqDto passwrdReqDto    ) {
        return service.forgotPassword(passwrdReqDto.getEmail());
    }


    @PostMapping("/send-email-otp/{id}")
    public StandardJsonResponse resendEmailOtp(@PathVariable UUID id) {
        User user = userService.findById(id);
        return userService.sendEmailOtp(user);
    }

    @PostMapping("/send-phone-otp/{id}")
    public StandardJsonResponse resendPhoneOtp(@PathVariable UUID id) {
        User user = userService.findById(id);
        return userService.sendPhoneOtp(user);
    }

    @PostMapping("/validate-email-otp")
    public StandardJsonResponse validateEmailOtp(@RequestBody DraftSecret draftSecret) {
        return userService.validateEmailOtp(draftSecret);
    }

    @PostMapping("/validate-phone-otp")
    public StandardJsonResponse validatePhoneOtp(@RequestBody DraftSecret draftSecret) {
        return userService.validatePhoneOtp(draftSecret);
    }

    @PostMapping("/update-password")
    public StandardJsonResponse updatePasssword(@RequestBody  @Valid DraftSecret draftSecret) {
        return userService.updatePasssword(draftSecret);
    }



}
