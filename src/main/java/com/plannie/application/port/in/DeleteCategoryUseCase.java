package com.plannie.application.port.in;

import com.plannie.domain.schedule.Category;

/**
 * 카테고리 삭제 유스케이스
 */
public interface DeleteCategoryUseCase {
    void deleteCategory(Long categoryId, Long userId);
}
