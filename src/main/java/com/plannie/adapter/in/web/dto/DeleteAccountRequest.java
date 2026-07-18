package com.plannie.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank(message = "비밀번호는 필수입니다")
        String password
) {}
