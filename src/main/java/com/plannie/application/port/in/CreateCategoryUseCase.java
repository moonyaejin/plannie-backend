package com.plannie.application.port.in;

import com.plannie.domain.schedule.Category;

/**
 * 카테고리 생성 유스케이스
 */
public interface CreateCategoryUseCase {
    Category createCategory(CreateCategoryCommand command);

    record CreateCategoryCommand(
            Long userId,
            String name,
            String color
    ) {}
}
