package com.plannie.adapter.in.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record WeeklyReportRequest(
        @NotNull LocalDate weekStart,
        @NotNull LocalDate weekEnd
) {
    @AssertTrue(message = "weekEnd는 weekStart 이후여야 합니다")
    public boolean isValidDateRange() {
        return weekStart == null || weekEnd == null || !weekEnd.isBefore(weekStart);
    }
}
