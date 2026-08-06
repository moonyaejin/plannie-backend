package com.plannie.application.service;

import com.plannie.application.port.in.StudySessionUseCase;
import com.plannie.application.port.out.LoadCategoryPort;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.application.port.out.StudySessionPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.schedule.Category;
import com.plannie.domain.schedule.RepeatRule;
import com.plannie.domain.schedule.Schedule;
import com.plannie.domain.studysession.StudySession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Tag("fast")
@ExtendWith(MockitoExtension.class)
class StudySessionServiceTest {

    @Mock private StudySessionPort studySessionPort;
    @Mock private LoadCategoryPort loadCategoryPort;
    @Mock private LoadSchedulePort loadSchedulePort;
    @InjectMocks private StudySessionService studySessionService;

    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 10L;
    private static final Long SCHEDULE_ID = 100L;

    private Category category() {
        return Category.builder().id(CATEGORY_ID).userId(USER_ID).name("수학").color("#FF0000").build();
    }

    private Schedule scheduleWithCategory() {
        return Schedule.builder()
                .id(SCHEDULE_ID).userId(USER_ID).title("기출 1회")
                .startDate(LocalDate.now()).endDate(LocalDate.now())
                .categoryId(CATEGORY_ID)
                .completed(false).repeatRule(RepeatRule.none())
                .build();
    }

    private StudySession activeSession() {
        return StudySession.builder()
                .id(1L).userId(USER_ID).categoryId(CATEGORY_ID)
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .build();
    }

    private StudySession stoppedSession() {
        StudySession s = activeSession();
        s.stop(LocalDateTime.now());
        return s;
    }

    // ==================== 세션 시작 ====================

    @Test
    @DisplayName("진행 중인 세션이 없으면 새 세션을 시작한다")
    void 세션_시작_성공() {
        given(loadCategoryPort.findByIdAndUserIdOrDefault(CATEGORY_ID, USER_ID)).willReturn(Optional.of(category()));
        given(studySessionPort.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
        given(studySessionPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        StudySession result = studySessionService.start(
                new StudySessionUseCase.StartCommand(USER_ID, CATEGORY_ID, null)
        );

        assertThat(result.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(result.isActive()).isTrue();
        verify(studySessionPort).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 카테고리면 CATEGORY_NOT_FOUND 예외 발생")
    void 없는_카테고리로_세션_시작_예외() {
        given(loadCategoryPort.findByIdAndUserIdOrDefault(CATEGORY_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studySessionService.start(
                new StudySessionUseCase.StartCommand(USER_ID, CATEGORY_ID, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("기본 카테고리(userId=null)로도 세션을 시작할 수 있다")
    void 기본_카테고리로_세션_시작_성공() {
        Category defaultCategory = Category.builder().id(99L).userId(null).name("기본").color("#4183F3").build();
        given(loadCategoryPort.findByIdAndUserIdOrDefault(99L, USER_ID)).willReturn(Optional.of(defaultCategory));
        given(studySessionPort.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
        given(studySessionPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        StudySession result = studySessionService.start(
                new StudySessionUseCase.StartCommand(USER_ID, 99L, null)
        );

        assertThat(result.getCategoryId()).isEqualTo(99L);
        verify(studySessionPort).save(any());
    }

    @Test
    @DisplayName("이미 진행 중인 세션이 있으면 STUDY_SESSION_ALREADY_ACTIVE 예외 발생")
    void 세션_중복_시작_예외() {
        given(loadCategoryPort.findByIdAndUserIdOrDefault(CATEGORY_ID, USER_ID)).willReturn(Optional.of(category()));
        given(studySessionPort.findActiveByUserId(USER_ID)).willReturn(Optional.of(activeSession()));

        assertThatThrownBy(() -> studySessionService.start(
                new StudySessionUseCase.StartCommand(USER_ID, CATEGORY_ID, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_SESSION_ALREADY_ACTIVE);
    }

    // ==================== 세션 종료 ====================

    @Test
    @DisplayName("진행 중인 세션을 종료하면 durationMinutes가 계산된다")
    void 세션_종료_성공() {
        given(studySessionPort.findById(1L)).willReturn(Optional.of(activeSession()));
        given(studySessionPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        StudySession result = studySessionService.stop(1L, USER_ID);

        assertThat(result.isActive()).isFalse();
        assertThat(result.getDurationMinutes()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("이미 종료된 세션을 종료하면 STUDY_SESSION_NOT_ACTIVE 예외 발생")
    void 이미_종료된_세션_종료_예외() {
        given(studySessionPort.findById(1L)).willReturn(Optional.of(stoppedSession()));

        assertThatThrownBy(() -> studySessionService.stop(1L, USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_SESSION_NOT_ACTIVE);
    }

    // ==================== 요약 ====================

    @Test
    @DisplayName("기간별 카테고리별 총 공부 시간을 내림차순으로 반환한다")
    void 카테고리별_요약_정렬() {
        Long mathId = 10L;
        Long engId = 11L;

        StudySession math = StudySession.builder().id(1L).userId(USER_ID).categoryId(mathId)
                .startedAt(LocalDateTime.now().minusHours(2)).endedAt(LocalDateTime.now().minusHours(1))
                .durationMinutes(60).build();
        StudySession english = StudySession.builder().id(2L).userId(USER_ID).categoryId(engId)
                .startedAt(LocalDateTime.now().minusHours(1)).endedAt(LocalDateTime.now())
                .durationMinutes(30).build();

        given(studySessionPort.findByUserIdAndDateRange(any(), any(), any()))
                .willReturn(List.of(math, english));
        given(loadCategoryPort.findAllByUserIdOrDefault(USER_ID)).willReturn(List.of(
                Category.builder().id(mathId).userId(USER_ID).name("수학").color("#FF0000").build(),
                Category.builder().id(engId).userId(USER_ID).name("영어").color("#00FF00").build()
        ));

        List<StudySessionUseCase.CategorySummary> result =
                studySessionService.getSummary(USER_ID, LocalDate.now(), LocalDate.now());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).categoryName()).isEqualTo("수학");
        assertThat(result.get(1).categoryName()).isEqualTo("영어");
    }

    // ==================== 일정 단위 타이머 ====================

    @Test
    @DisplayName("scheduleId로 시작하면 그 일정의 카테고리가 세션에 자동으로 설정된다")
    void 일정단위_타이머_시작_성공() {
        given(loadSchedulePort.findByIdAndUserId(SCHEDULE_ID, USER_ID)).willReturn(Optional.of(scheduleWithCategory()));
        given(loadCategoryPort.findByIdAndUserIdOrDefault(CATEGORY_ID, USER_ID)).willReturn(Optional.of(category()));
        given(studySessionPort.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
        given(studySessionPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        StudySession result = studySessionService.start(
                new StudySessionUseCase.StartCommand(USER_ID, null, SCHEDULE_ID)
        );

        assertThat(result.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(result.getScheduleId()).isEqualTo(SCHEDULE_ID);
    }

    @Test
    @DisplayName("존재하지 않는 scheduleId로 시작하면 SCHEDULE_NOT_FOUND 예외 발생")
    void 없는_일정으로_시작_예외() {
        given(loadSchedulePort.findByIdAndUserId(SCHEDULE_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studySessionService.start(
                new StudySessionUseCase.StartCommand(USER_ID, null, SCHEDULE_ID)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("카테고리가 없는 일정으로 시작하면 SCHEDULE_CATEGORY_REQUIRED 예외 발생")
    void 카테고리없는_일정으로_시작_예외() {
        Schedule noCategory = Schedule.builder()
                .id(SCHEDULE_ID).userId(USER_ID).title("기출 1회")
                .startDate(LocalDate.now()).endDate(LocalDate.now())
                .completed(false).repeatRule(RepeatRule.none())
                .build();
        given(loadSchedulePort.findByIdAndUserId(SCHEDULE_ID, USER_ID)).willReturn(Optional.of(noCategory));

        assertThatThrownBy(() -> studySessionService.start(
                new StudySessionUseCase.StartCommand(USER_ID, null, SCHEDULE_ID)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SCHEDULE_CATEGORY_REQUIRED);
    }

    // ==================== 일정별 요약 ====================

    @Test
    @DisplayName("일정별 요약은 scheduleId가 있는 세션만 집계하고 카테고리 직접 타이머는 제외한다")
    void 일정별_요약() {
        StudySession scheduleSession = StudySession.builder()
                .id(1L).userId(USER_ID).categoryId(CATEGORY_ID).scheduleId(SCHEDULE_ID)
                .startedAt(LocalDateTime.now().minusHours(1)).endedAt(LocalDateTime.now())
                .durationMinutes(30).build();
        StudySession categoryDirectSession = StudySession.builder()
                .id(2L).userId(USER_ID).categoryId(CATEGORY_ID)
                .startedAt(LocalDateTime.now().minusHours(2)).endedAt(LocalDateTime.now().minusHours(1))
                .durationMinutes(45).build();

        given(studySessionPort.findByUserIdAndDateRange(any(), any(), any()))
                .willReturn(List.of(scheduleSession, categoryDirectSession));
        given(loadSchedulePort.findByIdAndUserId(SCHEDULE_ID, USER_ID)).willReturn(Optional.of(scheduleWithCategory()));

        List<StudySessionUseCase.ScheduleSummary> result =
                studySessionService.getScheduleSummary(USER_ID, LocalDate.now(), LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).scheduleId()).isEqualTo(SCHEDULE_ID);
        assertThat(result.get(0).scheduleTitle()).isEqualTo("기출 1회");
        assertThat(result.get(0).totalMinutes()).isEqualTo(30);
    }
}
