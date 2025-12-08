package com.plannie.application.port.out;

import com.plannie.domain.schedule.Category;

/**
 * 카테고리 저장/삭제용 Port 인터페이스
 */
public interface SaveCategoryPort {

    Category save(Category category);

    void delete(Long categoryId);
}
