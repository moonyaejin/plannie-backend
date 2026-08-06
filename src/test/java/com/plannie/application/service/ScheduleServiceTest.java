package com.plannie.application.service;

import com.plannie.application.port.in.CreateScheduleUseCase.CreateScheduleCommand;
import com.plannie.application.port.in.ScheduleView;
import com.plannie.application.port.in.UpdateScheduleUseCase.UpdateScheduleCommand;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.application.port.out.SaveSchedulePort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.common.exception.ScheduleConflictException;
import com.plannie.domain.schedule.RepeatRule;
import com.plannie.domain.schedule.Schedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("fast")
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private LoadSchedulePort loadSchedulePort;

    @Mock
    private SaveSchedulePort saveSchedulePort;

    @InjectMocks
    private ScheduleService scheduleService;

    private static final Long USER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 4, 10);
    private static final LocalTime START = LocalTime.of(20, 0);
    private static final LocalTime END = LocalTime.of(22, 0);

    private CreateScheduleCommand command(LocalTime start, LocalTime end) {
        return new CreateScheduleCommand(USER_ID, "제목", null, DATE, null, start, end, null, null, null, null, null);
    }

    private Schedule savedSchedule() {
        return Schedule.builder()
                .id(1L).userId(USER_ID).title("제목")
                .startDate(DATE).endDate(DATE)
                .startTime(START).endTime(END)
                .completed(false).repeatRule(RepeatRule.none()).build();
    }

    private Schedule repeatingSchedule() {
        return Schedule.builder()
                .id(2L).userId(USER_ID).title("반복 일정")
                .startDate(DATE).endDate(DATE)
                .startTime(START).endTime(END)
                .completed(false).repeatRule(RepeatRule.daily(null)).build();
    }

    // ==================== 일정 생성 ====================

    @Test
    @DisplayName("정상 입력이면 일정을 저장하고 반환한다")
    void 일정_생성_성공() {
        given(loadSchedulePort.findConflictingSchedules(any(), any(), any(), any()))
                .willReturn(List.of());
        given(saveSchedulePort.save(any())).willReturn(savedSchedule());

        Schedule result = scheduleService.createSchedule(command(START, END));

        assertThat(result.getId()).isEqualTo(1L);
        verify(saveSchedulePort).save(any());
    }

    @Test
    @DisplayName("시작 시간이 종료 시간보다 늦으면 INVALID_TIME_RANGE 예외가 발생한다")
    void 시간_역순_입력시_예외() {
        assertThatThrownBy(() ->
                scheduleService.createSchedule(command(LocalTime.of(22, 0), LocalTime.of(20, 0)))
        )
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TIME_RANGE);

        verify(saveSchedulePort, never()).save(any());
    }

    @Test
    @DisplayName("시간이 겹치는 일정이 있으면 ScheduleConflictException이 발생한다")
    void 시간_충돌시_예외() {
        given(loadSchedulePort.findConflictingSchedules(any(), any(), any(), any()))
                .willReturn(List.of(savedSchedule()));

        assertThatThrownBy(() ->
                scheduleService.createSchedule(command(START, END))
        ).isInstanceOf(ScheduleConflictException.class);

        verify(saveSchedulePort, never()).save(any());
    }

    @Test
    @DisplayName("endDate가 null이면 startDate로 채워진다")
    void endDate_null이면_startDate로_채워짐() {
        given(loadSchedulePort.findConflictingSchedules(any(), any(), any(), any()))
                .willReturn(List.of());
        given(saveSchedulePort.save(any())).willAnswer(inv -> inv.getArgument(0));

        scheduleService.createSchedule(command(START, END));

        verify(saveSchedulePort).save(org.mockito.Mockito.argThat(s ->
                s.getEndDate().equals(s.getStartDate())
        ));
    }

    // ==================== 일정 수정 ====================

    @Test
    @DisplayName("존재하지 않는 일정을 수정하면 SCHEDULE_NOT_FOUND 예외가 발생한다")
    void 없는_일정_수정시_예외() {
        given(loadSchedulePort.findByIdAndUserId(99L, USER_ID)).willReturn(Optional.empty());

        UpdateScheduleCommand cmd = new UpdateScheduleCommand(
                99L, USER_ID, "수정", null, DATE, null, START, END, null, null, null
        );

        assertThatThrownBy(() -> scheduleService.updateSchedule(cmd))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
    }

    // ==================== 일정 삭제 ====================

    @Test
    @DisplayName("존재하지 않는 일정 삭제 요청은 SCHEDULE_NOT_FOUND 예외가 발생한다")
    void 없는_일정_삭제_예외() {
        given(loadSchedulePort.findByIdAndUserId(99L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.deleteSchedule(99L, USER_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("일정 삭제 시 해당 ID로 delete가 호출된다")
    void 일정_삭제_성공() {
        given(loadSchedulePort.findByIdAndUserId(1L, USER_ID))
                .willReturn(Optional.of(savedSchedule()));

        scheduleService.deleteSchedule(1L, USER_ID, null);

        verify(saveSchedulePort).delete(1L);
    }

    // ==================== 날짜별 조회: 반복 일정 완료 상태 동기화 ====================

    @Test
    @DisplayName("날짜별 조회 시 반복 일정의 완료 상태는 ScheduleCompletion 기준으로 반영된다")
    void 날짜별_조회시_반복일정_완료상태_반영() {
        Schedule repeating = repeatingSchedule();

        given(loadSchedulePort.findOneTimeSchedulesByDateRange(USER_ID, DATE, DATE))
                .willReturn(List.of());
        given(loadSchedulePort.findRepeatingSchedules(USER_ID))
                .willReturn(List.of(repeating));
        given(loadSchedulePort.findExceptions(List.of(2L), DATE, DATE))
                .willReturn(Map.of());
        given(loadSchedulePort.findCompletions(List.of(2L), DATE, DATE))
                .willReturn(Map.of("2_" + DATE, true));

        List<ScheduleView> result = scheduleService.getSchedulesByDate(USER_ID, DATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isCompleted()).isTrue();
    }

    // ==================== 반복 일정 완료 토글 이원화 방지 ====================

    @Test
    @DisplayName("반복 일정에 toggleComplete를 호출하면 예외가 발생한다")
    void 반복일정_toggleComplete_예외() {
        given(loadSchedulePort.findByIdAndUserId(2L, USER_ID))
                .willReturn(Optional.of(repeatingSchedule()));

        assertThatThrownBy(() -> scheduleService.toggleComplete(2L, USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECURRING_SCHEDULE_TOGGLE_NOT_ALLOWED);

        verify(saveSchedulePort, never()).toggleComplete(any());
    }

    // ==================== 반복 일정 occurrence 단위 수정/삭제 ====================

    @Test
    @DisplayName("occurrenceDate 지정 시 반복 일정은 해당 날짜만 수정되고 시리즈는 그대로다")
    void 반복일정_occurrence_수정() {
        given(loadSchedulePort.findByIdAndUserId(2L, USER_ID))
                .willReturn(Optional.of(repeatingSchedule()));

        UpdateScheduleCommand cmd = new UpdateScheduleCommand(
                2L, USER_ID, "이 날만 수정", null, DATE, null, START, END, null, null, DATE
        );

        scheduleService.updateSchedule(cmd);

        verify(saveSchedulePort).saveOccurrenceModification(2L, DATE, "이 날만 수정", null, START, END);
        verify(saveSchedulePort, never()).save(any());
    }

    @Test
    @DisplayName("occurrenceDate 없이 반복 일정을 수정하면 시리즈 전체가 수정된다")
    void 반복일정_전체_수정() {
        given(loadSchedulePort.findByIdAndUserId(2L, USER_ID))
                .willReturn(Optional.of(repeatingSchedule()));
        given(saveSchedulePort.save(any())).willAnswer(inv -> inv.getArgument(0));

        UpdateScheduleCommand cmd = new UpdateScheduleCommand(
                2L, USER_ID, "전체 수정", null, DATE, null, START, END, null, null, null
        );

        scheduleService.updateSchedule(cmd);

        verify(saveSchedulePort).save(any());
        verify(saveSchedulePort, never()).saveOccurrenceModification(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("occurrenceDate 지정 시 반복 일정은 해당 날짜만 삭제되고 시리즈는 남는다")
    void 반복일정_occurrence_삭제() {
        given(loadSchedulePort.findByIdAndUserId(2L, USER_ID))
                .willReturn(Optional.of(repeatingSchedule()));

        scheduleService.deleteSchedule(2L, USER_ID, DATE);

        verify(saveSchedulePort).deleteOccurrence(2L, DATE);
        verify(saveSchedulePort, never()).delete(any());
    }

    @Test
    @DisplayName("occurrenceDate 없이 반복 일정을 삭제하면 시리즈 전체가 삭제된다")
    void 반복일정_전체_삭제() {
        given(loadSchedulePort.findByIdAndUserId(2L, USER_ID))
                .willReturn(Optional.of(repeatingSchedule()));

        scheduleService.deleteSchedule(2L, USER_ID, null);

        verify(saveSchedulePort).delete(2L);
        verify(saveSchedulePort, never()).deleteOccurrence(any(), any());
    }

    // ==================== 시간 없는 일정 ====================

    @Test
    @DisplayName("시간 없이 일정을 생성하면 충돌 검사 없이 저장된다")
    void 시간없는_일정_생성() {
        given(saveSchedulePort.save(any())).willAnswer(inv -> inv.getArgument(0));

        Schedule result = scheduleService.createSchedule(command(null, null));

        assertThat(result.getStartTime()).isNull();
        assertThat(result.getEndTime()).isNull();
        verify(loadSchedulePort, never()).findConflictingSchedules(any(), any(), any(), any());
        verify(saveSchedulePort).save(any());
    }

    @Test
    @DisplayName("시작 시간만 있고 종료 시간이 없으면 SCHEDULE_TIME_PARTIALLY_SET 예외가 발생한다")
    void 시간_한쪽만_입력시_예외() {
        assertThatThrownBy(() ->
                scheduleService.createSchedule(command(START, null))
        )
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SCHEDULE_TIME_PARTIALLY_SET);

        verify(saveSchedulePort, never()).save(any());
    }

    @Test
    @DisplayName("같은 날짜 조회 시 시간 없는 일정이 시간 있는 일정보다 앞에 정렬된다")
    void 시간없는_일정이_먼저_정렬된다() {
        Schedule noTime = Schedule.builder()
                .id(3L).userId(USER_ID).title("시간 없는 일정")
                .startDate(DATE).endDate(DATE)
                .completed(false).repeatRule(RepeatRule.none()).build();
        Schedule timed = savedSchedule();

        given(loadSchedulePort.findOneTimeSchedulesByDateRange(USER_ID, DATE, DATE))
                .willReturn(List.of(timed, noTime));
        given(loadSchedulePort.findRepeatingSchedules(USER_ID))
                .willReturn(List.of());

        List<ScheduleView> result = scheduleService.getSchedulesByDate(USER_ID, DATE);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(3L);
        assertThat(result.get(1).getId()).isEqualTo(1L);
    }
}
