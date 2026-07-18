package com.plannie.application.service;

import com.plannie.application.port.out.LoadUserPort;
import com.plannie.application.port.out.UserManagementPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("fast")
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private UserManagementPort userManagementPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileService userProfileService;

    private static final Long USER_ID = 1L;

    private User user() {
        return User.builder()
                .id(USER_ID).email("test@plannie.com")
                .password("encoded-password").nickname("테스터")
                .build();
    }

    @Test
    @DisplayName("존재하지 않는 유저를 탈퇴 시도하면 USER_NOT_FOUND 예외가 발생한다")
    void 없는_유저_탈퇴시_예외() {
        given(loadUserPort.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.deleteAccount(USER_ID, "아무값"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userManagementPort, never()).deleteById(any());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 INVALID_PASSWORD 예외가 발생하고 탈퇴되지 않는다")
    void 비밀번호_불일치시_탈퇴되지않음() {
        given(loadUserPort.findById(USER_ID)).willReturn(Optional.of(user()));
        given(passwordEncoder.matches("틀린비밀번호", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> userProfileService.deleteAccount(USER_ID, "틀린비밀번호"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD);

        verify(userManagementPort, never()).deleteById(any());
    }

    @Test
    @DisplayName("비밀번호가 일치하면 계정이 삭제된다")
    void 비밀번호_일치시_탈퇴성공() {
        given(loadUserPort.findById(USER_ID)).willReturn(Optional.of(user()));
        given(passwordEncoder.matches("맞는비밀번호", "encoded-password")).willReturn(true);

        userProfileService.deleteAccount(USER_ID, "맞는비밀번호");

        verify(userManagementPort).deleteById(USER_ID);
    }
}
