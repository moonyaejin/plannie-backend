package com.plannie.adapter.in.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record StudyPlanRequest(
        @NotBlank String examName,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull @FutureOrPresent LocalDate examDate,
        String textbook,
        @Positive int dailyHours,
        int pastExamRounds,
        List<String> focusAreas
) {
    @AssertTrue(message = "시험일은 시작일 이후여야 합니다")
    public boolean isValidDateRange() {
        return startDate == null || examDate == null || examDate.isAfter(startDate);
    }
}
