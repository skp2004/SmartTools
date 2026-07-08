package com.smarttools.invoice.controller;

import com.smarttools.invoice.entity.User;
import com.smarttools.invoice.repository.UserRepository;
import com.smarttools.invoice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<User> getMe(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateMe(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                         @RequestBody User userUpdate) {
        return userRepository.findById(userPrincipal.getId())
                .map(user -> {
                    user.setName(userUpdate.getName());
                    user.setPictureUrl(userUpdate.getPictureUrl());
                    return ResponseEntity.ok(userRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
