package com.plannie.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequest {

    public record Register(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") String password,
            @NotBlank @Size(max = 20) String nickname
    ) {}

    public record Login(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}
}
