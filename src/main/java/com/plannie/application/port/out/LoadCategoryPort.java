package com.plannie.application.port.out;

import com.plannie.domain.schedule.Category;

import java.util.List;
import java.util.Optional;

/**
 * 카테고리 조회용 Port 인터페이스
 */
public interface LoadCategoryPort {
    Optional<Category> findById(Long id);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    List<Category> findAllByUserIdOrDefault(Long userId);
}
