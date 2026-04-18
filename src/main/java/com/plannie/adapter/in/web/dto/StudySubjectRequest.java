package com.plannie.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudySubjectRequest(
        @NotBlank(message = "과목명은 필수입니다")
        @Size(max = 100, message = "과목명은 100자 이하여야 합니다")
        String name,

        @Size(max = 20, message = "색상 코드는 20자 이하여야 합니다")
        String color  // 예: "#FF5733", null 가능
) {}
