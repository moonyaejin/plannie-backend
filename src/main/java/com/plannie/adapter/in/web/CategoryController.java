package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.CategoryRequest;
import com.plannie.adapter.in.web.dto.CategoryResponse;
import com.plannie.application.port.in.CreateCategoryUseCase;
import com.plannie.application.port.in.CreateCategoryUseCase.CreateCategoryCommand;
import com.plannie.application.port.in.DeleteCategoryUseCase;
import com.plannie.application.port.in.GetCategoryUseCase;
import com.plannie.application.port.in.UpdateCategoryUseCase;
import com.plannie.application.port.in.UpdateCategoryUseCase.UpdateCategoryCommand;
import com.plannie.domain.schedule.Category;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Category REST Controller
 *
 * 역할:
 * - HTTP 요청 수신 및 응답
 * - 요청 데이터 검증 (@Valid)
 * - DTO ↔ Command 변환
 * - UseCase 호출
 *
 * 의존성:
 * - UseCase 인터페이스만 의존 (Service 구현체 직접 의존 X)
 * - 이렇게 하면 Controller 테스트할 때 UseCase Mock 주입 가능
 *
 * REST API 설계 원칙:
 * - GET: 조회 (멱등성 O)
 * - POST: 생성 (멱등성 X)
 * - PUT: 수정 (멱등성 O)
 * - DELETE: 삭제 (멱등성 O)
 */
@Tag(name = "Category", description = "카테고리 관리 API")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final GetCategoryUseCase getCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    private static final String USER_ID_HEADER = "X-User-Id";

    /**
     * 카테고리 생성
     * POST /api/categories
     */
    @Operation(summary = "카테고리 생성", description = "새로운 카테고리를 생성합니다")
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CategoryRequest request) {

        CreateCategoryCommand command = new CreateCategoryCommand(
                userId,
                request.name(),
                request.color()
        );

        Category created = createCategoryUseCase.createCategory(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CategoryResponse.from(created));
    }

    /**
     * 카테고리 목록 조회
     * GET /api/categories
     *
     * 기본 카테고리 + 유저의 커스텀 카테고리 반환
     */
    @Operation(summary = "카테고리 목록 조회", description = "기본 카테고리와 유저의 커스텀 카테고리를 조회합니다")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @RequestHeader(USER_ID_HEADER) Long userId) {

        List<Category> categories = getCategoryUseCase.getCategories(userId);

        List<CategoryResponse> response = categories.stream()
                .map(CategoryResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 카테고리 수정
     * PUT /api/categories/{id}
     */
    @Operation(summary = "카테고리 수정", description = "카테고리를 수정합니다")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Parameter(description = "카테고리 ID") @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {

        UpdateCategoryCommand command = new UpdateCategoryCommand(
                id,
                userId,
                request.name(),
                request.color()
        );

        Category updated = updateCategoryUseCase.updateCategory(command);

        return ResponseEntity.ok(CategoryResponse.from(updated));
    }

    /**
     * 카테고리 삭제
     * DELETE /api/categories/{id}
     */
    @Operation(summary = "카테고리 삭제", description = "카테고리를 삭제합니다")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Parameter(description = "카테고리 ID") @PathVariable Long id) {

        deleteCategoryUseCase.deleteCategory(id, userId);

        return ResponseEntity.noContent().build();
    }
}