package com.plannie.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudySessionRequest(
        @NotBlank(message = "과목명은 필수입니다")
        @Size(max = 100, message = "과목명은 100자 이하여야 합니다")
        String subject,

        Long categoryId  // null 가능
) {}
