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

/**
 * Category Service
 *
 * 역할:
 * - CreateCategoryUseCase, GetCategoryUseCase, UpdateCategoryUseCase, DeleteCategoryUseCase 구현
 * - 비즈니스 로직 처리 (권한 검증, 기본 카테고리 보호 등)
 * - 트랜잭션 관리
 *
 * 의존성:
 * - Port 인터페이스만 의존 (LoadCategoryPort, SaveCategoryPort)
 * - JPA Repository를 직접 사용하지 않음!
 * - 이렇게 하면 테스트할 때 Port를 Mock으로 쉽게 대체 가능
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService implements CreateCategoryUseCase,
        GetCategoryUseCase,
        UpdateCategoryUseCase,
        DeleteCategoryUseCase {

    private final LoadCategoryPort loadCategoryPort;
    private final SaveCategoryPort saveCategoryPort;

    // ==================== CreateCategoryUseCase 구현 ====================

    /**
     * 카테고리 생성
     * <p>
     * 처리 순서:
     * 1. 도메인 객체 생성
     * 2. DB 저장
     */
    @Override
    @Transactional
    public Category createCategory(CreateCategoryCommand command) {
        // 도메인 객체 생성
        Category category = Category.builder()
                .userId(command.userId())
                .name(command.name())
                .color(command.color())
                .build();

        // DB 저장 및 반환
        return saveCategoryPort.save(category);
    }

    // ==================== GetCategoryUseCase 구현 ====================

    @Override
    public List<Category> getCategories(Long userId) {
        return loadCategoryPort.findAllByUserIdOrDefault(userId);
    }

    // ==================== UpdateCategoryUseCase 구현 ====================
    @Override
    @Transactional
    public Category updateCategory(UpdateCategoryCommand command) {
        // 기존 카테고리 조회
        Category existingCategory = loadCategoryPort
                .findByIdAndUserId(command.categoryId(), command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 카테고리 정보 수정
        existingCategory.update(command.categoryName(), command.color());

        // DB 저장
        return saveCategoryPort.save(existingCategory);
    }

    // ==================== DeleteCategoryUseCase 구현 ====================

    @Override
    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        Category category = loadCategoryPort
                .findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        saveCategoryPort.delete(categoryId);
    }
}
