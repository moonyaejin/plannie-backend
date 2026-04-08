package com.plannie.adapter.out.persistence;

import com.plannie.adapter.out.persistence.entity.CategoryJpaEntity;
import com.plannie.adapter.out.persistence.repository.CategoryJpaRepository;
import com.plannie.application.port.out.LoadCategoryPort;
import com.plannie.application.port.out.SaveCategoryPort;
import com.plannie.domain.schedule.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoryPersistenceAdapter implements LoadCategoryPort, SaveCategoryPort {

    private final CategoryJpaRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    // ==================== LoadCategoryPort 구현 ====================

    @Override
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::toDomain);
    }

    @Override
    public Optional<Category> findByIdAndUserId(Long id, Long userId) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .map(categoryMapper::toDomain);
    }

    @Override
    public List<Category> findAllByUserIdOrDefault(Long userId) {
        return categoryRepository.findAllByUserIdOrDefault(userId)
                .stream()
                .map(categoryMapper::toDomain)
                .toList();
    }

    // ==================== SaveCategoryPort 구현 ====================
    @Override
    public Category save(Category category) {
        CategoryJpaEntity entity = categoryMapper.toEntity(category);
        CategoryJpaEntity savedEntity = categoryRepository.save(entity);
        return categoryMapper.toDomain(savedEntity);
    }

    @Override
    public void delete(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }
}