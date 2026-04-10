package com.plannie.application.port.in;

public interface AuthUseCase {

    TokenResult register(RegisterCommand command);

    TokenResult login(LoginCommand command);

    record RegisterCommand(String email, String password, String nickname) {}

    record LoginCommand(String email, String password) {}

    record TokenResult(Long userId, String email, String nickname, String accessToken) {}
}
