package com.plannie.application.service;

import com.plannie.application.port.in.StudySessionUseCase;
import com.plannie.application.port.out.StudySessionPort;
import com.plannie.application.port.out.StudySubjectPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.studysession.StudySession;
import com.plannie.domain.studysession.StudySubject;
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
    @Mock private StudySubjectPort studySubjectPort;
    @InjectMocks private StudySessionService studySessionService;

    private static final Long USER_ID = 1L;
    private static final Long SUBJECT_ID = 10L;

    private StudySubject subject() {
        return StudySubject.builder().id(SUBJECT_ID).userId(USER_ID).name("수학").color("#FF0000").build();
    }

    private StudySession activeSession() {
        return StudySession.builder()
                .id(1L).userId(USER_ID).subjectId(SUBJECT_ID)
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
        given(studySubjectPort.findByIdAndUserId(SUBJECT_ID, USER_ID)).willReturn(Optional.of(subject()));
        given(studySessionPort.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
        given(studySessionPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        StudySession result = studySessionService.start(
                new StudySessionUseCase.StartCommand(USER_ID, SUBJECT_ID)
        );

        assertThat(result.getSubjectId()).isEqualTo(SUBJECT_ID);
        assertThat(result.isActive()).isTrue();
        verify(studySessionPort).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 과목이면 STUDY_SUBJECT_NOT_FOUND 예외 발생")
    void 없는_과목으로_세션_시작_예외() {
        given(studySubjectPort.findByIdAndUserId(SUBJECT_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> studySessionService.start(
                new StudySessionUseCase.StartCommand(USER_ID, SUBJECT_ID)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STUDY_SUBJECT_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 진행 중인 세션이 있으면 STUDY_SESSION_ALREADY_ACTIVE 예외 발생")
    void 세션_중복_시작_예외() {
        given(studySubjectPort.findByIdAndUserId(SUBJECT_ID, USER_ID)).willReturn(Optional.of(subject()));
        given(studySessionPort.findActiveByUserId(USER_ID)).willReturn(Optional.of(activeSession()));

        assertThatThrownBy(() -> studySessionService.start(
                new StudySessionUseCase.StartCommand(USER_ID, SUBJECT_ID)
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
    @DisplayName("기간별 과목별 총 공부 시간을 내림차순으로 반환한다")
    void 과목별_요약_정렬() {
        Long mathId = 10L;
        Long engId = 11L;

        StudySession math = StudySession.builder().id(1L).userId(USER_ID).subjectId(mathId)
                .startedAt(LocalDateTime.now().minusHours(2)).endedAt(LocalDateTime.now().minusHours(1))
                .durationMinutes(60).build();
        StudySession english = StudySession.builder().id(2L).userId(USER_ID).subjectId(engId)
                .startedAt(LocalDateTime.now().minusHours(1)).endedAt(LocalDateTime.now())
                .durationMinutes(30).build();

        given(studySessionPort.findByUserIdAndDateRange(any(), any(), any()))
                .willReturn(List.of(math, english));
        given(studySubjectPort.findAllByUserId(USER_ID)).willReturn(List.of(
                StudySubject.builder().id(mathId).userId(USER_ID).name("수학").color("#FF0000").build(),
                StudySubject.builder().id(engId).userId(USER_ID).name("영어").color("#00FF00").build()
        ));

        List<StudySessionUseCase.SubjectSummary> result =
                studySessionService.getSummary(USER_ID, LocalDate.now(), LocalDate.now());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).subjectName()).isEqualTo("수학");
        assertThat(result.get(1).subjectName()).isEqualTo("영어");
    }
}
