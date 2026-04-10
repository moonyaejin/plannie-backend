package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.AuthRequest;
import com.plannie.adapter.in.web.dto.AuthResponse;
import com.plannie.application.port.in.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "회원가입 / 로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입 후 JWT 토큰을 반환합니다.")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody AuthRequest.Register request) {
        return AuthResponse.from(
                authUseCase.register(new AuthUseCase.RegisterCommand(
                        request.email(), request.password(), request.nickname()
                ))
        );
    }

    @Operation(summary = "로그인", description = "이메일, 비밀번호로 로그인 후 JWT 토큰을 반환합니다.")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest.Login request) {
        return AuthResponse.from(
                authUseCase.login(new AuthUseCase.LoginCommand(
                        request.email(), request.password()
                ))
        );
    }
}
