package com.smarttools.invoice.controller;

import com.smarttools.invoice.entity.Conversion;
import com.smarttools.invoice.repository.ConversionRepository;
import com.smarttools.invoice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionRepository conversionRepository;

    @GetMapping
    public ResponseEntity<Page<Conversion>> getHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Conversion> history = conversionRepository.findByUserIdOrderByCreatedAtDesc(
                userPrincipal.getId(),
                PageRequest.of(page, size)
        );
        return ResponseEntity.ok(history);
    }
}
