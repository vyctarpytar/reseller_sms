package com.spa.smart_gate_springboot.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spa.smart_gate_springboot.config.JwtService;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.user.UsrStatus;
import com.spa.smart_gate_springboot.user.token.Token;
import com.spa.smart_gate_springboot.user.token.TokenRepository;
import com.spa.smart_gate_springboot.user.token.TokenType;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserService userService;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder().usrId(UUID.fromString(request.getUuid())).email(request.getEmail()).password(passwordEncoder.encode(request.getPassword())).layer(request.getLayers())
                .permissions(request.getPermissions()).phoneNumber(request.getPhonenumber()).brnId(request.getBrnid()).role(request.getRole()).build();
        var savedUser = userService.save(user);

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        saveUserToken(savedUser, jwtToken);
        return AuthenticationResponse.builder().accessToken(jwtToken).refreshToken(refreshToken).success(true).build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        var user = userService.findByEmail(request.getEmail());
        revokeAllUserTokens(user);
        if (user.getUsrStatus().equals(UsrStatus.ACTIVE)) {
            var jwtToken = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);
            saveUserToken(user, jwtToken);
            return AuthenticationResponse.builder().accessToken(jwtToken).refreshToken(refreshToken).success(true).build();
        }
        return AuthenticationResponse.builder().accessToken("USER STATUS IS "+user.getUsrStatus()).success(false).build();
    }

    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder().usrId(user.getUsrId()).token(jwtToken).tokenType(TokenType.BEARER).expired(false).revoked(false).build();
        tokenRepository.save(token);
    }

    public void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getUsrId());
        if (validUserTokens.isEmpty()) return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = userService.findByEmail(userEmail);
            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);
                var authResponse = AuthenticationResponse.builder().accessToken(accessToken).refreshToken(refreshToken).success(true).build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }

    public StandardJsonResponse forgotPassword(String email) {
        User stpUser = userService.findByEmail(email);
        return userService.sendEmailOtp(stpUser);
    }
}
