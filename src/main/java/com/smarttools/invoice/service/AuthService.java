package com.smarttools.invoice.service;

import com.smarttools.invoice.dto.request.LoginRequest;
import com.smarttools.invoice.dto.request.RegisterRequest;
import com.smarttools.invoice.dto.request.TokenRefreshRequest;
import com.smarttools.invoice.dto.response.JwtAuthResponse;
import com.smarttools.invoice.entity.*;
import com.smarttools.invoice.exception.BadRequestException;
import com.smarttools.invoice.exception.UnauthorizedException;
import com.smarttools.invoice.repository.CompanyRepository;
import com.smarttools.invoice.repository.SubscriptionRepository;
import com.smarttools.invoice.repository.UserRepository;
import com.smarttools.invoice.security.JwtTokenProvider;
import com.smarttools.invoice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new User and their Company in a single database transaction.
     */
    @Transactional
    public JwtAuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email address already in use.");
        }

        // 1. Create User
        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .authProvider(AuthProvider.LOCAL)
            .build();
        User savedUser = userRepository.save(user);

        // 2. Create Company
        Company company = Company.builder()
            .owner(savedUser)
            .name(request.getCompanyName())
            .build();
        Company savedCompany = companyRepository.save(company);
        savedUser.setCompany(savedCompany); // associate bidirectional back-reference

        // 3. Create Subscription (Default FREE plan)
        Subscription subscription = Subscription.builder()
            .user(savedUser)
            .plan(SubscriptionPlan.FREE)
            .status(SubscriptionStatus.ACTIVE)
            .build();
        subscriptionRepository.save(subscription);
        savedUser.setSubscription(subscription);

        log.info("Registered new user [{}] and company [{}]", savedUser.getEmail(), savedCompany.getName());

        // 4. Generate JWT tokens
        UserPrincipal userPrincipal = UserPrincipal.create(savedUser);
        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        return new JwtAuthResponse(
            accessToken,
            refreshToken,
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getName(),
            savedCompany.getId(),
            savedCompany.getName()
        );
    }

    /**
     * Authenticates User credentials and returns a JWT token pair.
     */
    public JwtAuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        // Load the database company name for response metadata
        User user = userRepository.findById(userPrincipal.getId())
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        String companyName = user.getCompany() != null ? user.getCompany().getName() : null;

        log.info("User [{}] logged in successfully", userPrincipal.getEmail());

        return new JwtAuthResponse(
            accessToken,
            refreshToken,
            userPrincipal.getId(),
            userPrincipal.getEmail(),
            userPrincipal.getName(),
            userPrincipal.getCompanyId(),
            companyName
        );
    }

    /**
     * Validates refresh token and generates a new access token.
     */
    public JwtAuthResponse refresh(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        Long userId = tokenProvider.getUserIdFromJwt(refreshToken);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UnauthorizedException("User not found for refresh token"));

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);

        String companyName = user.getCompany() != null ? user.getCompany().getName() : null;
        Long companyId = user.getCompany() != null ? user.getCompany().getId() : null;

        log.info("Refreshed access token for user [{}]", user.getEmail());

        return new JwtAuthResponse(
            newAccessToken,
            refreshToken, // return the same refresh token
            user.getId(),
            user.getEmail(),
            user.getName(),
            companyId,
            companyName
        );
    }
}
