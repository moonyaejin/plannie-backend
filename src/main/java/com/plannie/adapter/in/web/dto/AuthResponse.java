package com.plannie.adapter.in.web.dto;

import com.plannie.application.port.in.AuthUseCase.TokenResult;

public record AuthResponse(
        Long userId,
        String email,
        String nickname,
        String accessToken
) {
    public static AuthResponse from(TokenResult result) {
        return new AuthResponse(
                result.userId(),
                result.email(),
                result.nickname(),
                result.accessToken()
        );
    }
}
