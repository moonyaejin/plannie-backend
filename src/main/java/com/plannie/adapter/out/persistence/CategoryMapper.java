package com.plannie.adapter.out.persistence;

import com.plannie.adapter.out.persistence.entity.CategoryJpaEntity;
import com.plannie.domain.schedule.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    /**
     * JPA Entity → 도메인 모델 변환
     */
    public Category toDomain(CategoryJpaEntity category) {
        return Category.builder()
                .id(category.getId())
                .userId(category.getUserId())
                .name(category.getCategoryName())
                .color(category.getColor())
                .build();
    }

    /**
     * 도메인 모델 → JPA Entity 변환
     */
    public CategoryJpaEntity toEntity(Category category) {
        return CategoryJpaEntity.builder()
                .userId(category.getUserId())
                .categoryName(category.getName())
                .color(category.getColor())
                .build();
    }
}
