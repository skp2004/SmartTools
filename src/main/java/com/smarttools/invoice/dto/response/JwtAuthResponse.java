package com.smarttools.invoice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthResponse {

    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long userId;
    private String email;
    private String name;
    private String role;
    private String plan;
    private String pictureUrl;

    public JwtAuthResponse(String accessToken, String refreshToken, Long userId, String email, String name,
                           String role, String plan, String pictureUrl) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;
        this.plan = plan;
        this.pictureUrl = pictureUrl;
    }
}
