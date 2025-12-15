package com.plannie.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 카테고리 생성/수정 요청 DTO
 */
public record CategoryRequest(

        @NotBlank(message = "카테고리 이름은 필수입니다")
        @Size(max = 10, message = "카테고리 이름은 10자 이하여야 합니다")
        String name,

        @Size(max = 200, message = "색상 코드는 200자 이하여야 합니다")
        String color
) {
}