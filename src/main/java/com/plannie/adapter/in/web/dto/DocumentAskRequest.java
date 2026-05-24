package com.plannie.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record DocumentAskRequest(
        @NotBlank String question
) {}
