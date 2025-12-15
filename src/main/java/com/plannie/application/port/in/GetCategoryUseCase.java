package com.plannie.application.port.in;

import com.plannie.domain.schedule.Category;

import java.util.List;

/**
 * 카테고리 조회 유스케이스
 */
public interface GetCategoryUseCase {
    List<Category> getCategories(Long userId);
}
