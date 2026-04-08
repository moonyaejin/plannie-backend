package com.plannie.adapter.in.web.dto;

import com.plannie.domain.schedule.Category;

/**
 * 카테고리 응답 DTO
 */
public record CategoryResponse(
        Long id,
        Long userId,
        String name,
        String color
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getUserId(),
                category.getName(),
                category.getColor()
        );
    }
}