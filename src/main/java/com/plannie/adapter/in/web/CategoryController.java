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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Category", description = "카테고리 관리 API")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final GetCategoryUseCase getCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    @Operation(summary = "카테고리 생성", description = "새로운 카테고리를 생성합니다")
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @AuthenticationPrincipal Long userId,
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

    @Operation(summary = "카테고리 목록 조회", description = "기본 카테고리와 유저의 커스텀 카테고리를 조회합니다")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal Long userId) {

        List<Category> categories = getCategoryUseCase.getCategories(userId);

        List<CategoryResponse> response = categories.stream()
                .map(CategoryResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "카테고리 수정", description = "카테고리를 수정합니다")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @AuthenticationPrincipal Long userId,
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

    @Operation(summary = "카테고리 삭제", description = "카테고리를 삭제합니다")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "카테고리 ID") @PathVariable Long id) {

        deleteCategoryUseCase.deleteCategory(id, userId);

        return ResponseEntity.noContent().build();
    }
}