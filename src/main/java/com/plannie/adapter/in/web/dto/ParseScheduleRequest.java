package com.plannie.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ParseScheduleRequest(
        @NotNull Long userId,
        @NotBlank String text
) {}
