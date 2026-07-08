package com.smarttools.invoice.controller;

import com.smarttools.invoice.dto.request.LoginRequest;
import com.smarttools.invoice.dto.request.RegisterRequest;
import com.smarttools.invoice.dto.request.TokenRefreshRequest;
import com.smarttools.invoice.dto.response.JwtAuthResponse;
import com.smarttools.invoice.exception.ForbiddenException;
import com.smarttools.invoice.service.AuthService;
import com.smarttools.invoice.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final RateLimitingService rateLimitingService;

    @PostMapping("/register")
    public ResponseEntity<JwtAuthResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        JwtAuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> authenticateUser(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest servletRequest) {
        String clientIp = getClientIp(servletRequest);
        
        // Rate limit: max 5 login attempts per minute per IP
        if (!rateLimitingService.tryConsume(clientIp)) {
            log.warn("Rate limit exceeded for IP [{}] on /login endpoint", clientIp);
            throw new ForbiddenException("Too many login attempts. Please try again in a minute.");
        }

        JwtAuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        JwtAuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
