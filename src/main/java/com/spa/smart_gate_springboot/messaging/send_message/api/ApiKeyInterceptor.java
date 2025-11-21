package com.spa.smart_gate_springboot.messaging.send_message.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.TextUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyInterceptor implements HandlerInterceptor {
    private final ApiKeyRepository apiKeyRepository;

    public boolean validateApiKey(String apiKey) {
        boolean isValid = apiKeyRepository.existsValidApiKey(apiKey);
        log.info("Api key validation result intercepted :{}---- {}", apiKey, isValid);
        return isValid;
    }


//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        String apiKey = request.getHeader("X-API-KEY");
//        if (TextUtils.isEmpty(apiKey)) {
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.getWriter().write("Missing API Key : " + apiKey);
//            return false;
//        }
//
//        if (!validateApiKey(apiKey)) {
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.getWriter().write("Invalid API Key : " + apiKey);
//            return false;
//        }
//
//        return true;
//    }

}
