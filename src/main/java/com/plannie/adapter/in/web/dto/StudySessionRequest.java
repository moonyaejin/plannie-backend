package com.plannie.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record StudySessionRequest(
        @NotNull(message = "과목 ID는 필수입니다")
        Long subjectId
) {}
