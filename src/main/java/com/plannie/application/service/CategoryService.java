package com.plannie.application.service;

import com.plannie.application.port.in.CreateCategoryUseCase;
import com.plannie.application.port.in.DeleteCategoryUseCase;
import com.plannie.application.port.in.GetCategoryUseCase;
import com.plannie.application.port.in.UpdateCategoryUseCase;
import com.plannie.application.port.out.LoadCategoryPort;
import com.plannie.application.port.out.SaveCategoryPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.schedule.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService implements CreateCategoryUseCase,
        GetCategoryUseCase,
        UpdateCategoryUseCase,
        DeleteCategoryUseCase {

    private final LoadCategoryPort loadCategoryPort;
    private final SaveCategoryPort saveCategoryPort;

    @Override
    @Transactional
    public Category createCategory(CreateCategoryCommand command) {
        Category category = Category.builder()
                .userId(command.userId())
                .name(command.name())
                .color(command.color())
                .build();
        return saveCategoryPort.save(category);
    }

    @Override
    public List<Category> getCategories(Long userId) {
        return loadCategoryPort.findAllByUserIdOrDefault(userId);
    }

    @Override
    @Transactional
    public Category updateCategory(UpdateCategoryCommand command) {
        Category existingCategory = loadCategoryPort
                .findByIdAndUserId(command.categoryId(), command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        existingCategory.update(command.categoryName(), command.color());
        return saveCategoryPort.save(existingCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        loadCategoryPort.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        saveCategoryPort.delete(categoryId);
    }
}
