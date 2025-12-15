package com.plannie.application.port.in;

import com.plannie.domain.schedule.Category;

public interface UpdateCategoryUseCase {
    Category updateCategory(UpdateCategoryCommand command);

    record UpdateCategoryCommand(
            Long categoryId,
            Long userId,
            String categoryName,
            String color
    ) {}
}
