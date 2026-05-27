package com.plannie.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UserProfileRequest(
        @NotBlank String nickname,
        String phone,
        String address,
        LocalDate birth,
        String gender,
        String password
) {}
