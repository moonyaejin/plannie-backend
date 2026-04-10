package com.plannie.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record WeeklyFeedbackRequest(
        @NotNull LocalDate weekStart,
        @NotNull LocalDate weekEnd
) {}
