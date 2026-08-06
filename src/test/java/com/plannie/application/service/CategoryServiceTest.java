package com.plannie.application.service;

import com.plannie.application.port.in.CreateCategoryUseCase.CreateCategoryCommand;
import com.plannie.application.port.out.LoadCategoryPort;
import com.plannie.application.port.out.SaveCategoryPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.schedule.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("fast")
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private LoadCategoryPort loadCategoryPort;

    @Mock
    private SaveCategoryPort saveCategoryPort;

    @InjectMocks
    private CategoryService categoryService;

    private static final Long USER_ID = 1L;

    @Test
    @DisplayName("이름이 중복되지 않으면 카테고리가 생성된다")
    void 카테고리_생성_성공() {
        given(loadCategoryPort.existsByUserIdAndName(USER_ID, "공부")).willReturn(false);
        given(saveCategoryPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        Category result = categoryService.createCategory(
                new CreateCategoryCommand(USER_ID, "공부", "#FF5733")
        );

        assertThat(result.getName()).isEqualTo("공부");
        verify(saveCategoryPort).save(any());
    }

    @Test
    @DisplayName("같은 이름의 카테고리가 이미 있으면 CATEGORY_DUPLICATE 예외가 발생한다")
    void 이름_중복시_예외() {
        given(loadCategoryPort.existsByUserIdAndName(USER_ID, "공부")).willReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(
                new CreateCategoryCommand(USER_ID, "공부", "#FF5733")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_DUPLICATE);

        verify(saveCategoryPort, never()).save(any());
    }
}
