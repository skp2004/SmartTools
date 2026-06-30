package com.smarttools.invoice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple health-check and status controller for Phase 1 verification.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Invoice SaaS API"
        ));
    }

    @GetMapping("/test/protected")
    public ResponseEntity<Map<String, String>> protectedEndpoint() {
        return ResponseEntity.ok(Map.of(
            "message", "You have accessed a protected endpoint. Auth is working!"
        ));
    }
}
