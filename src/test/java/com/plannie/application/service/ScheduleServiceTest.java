package com.plannie.application.service;

import com.plannie.application.port.in.CreateScheduleUseCase.CreateScheduleCommand;
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
                99L, USER_ID, "수정", null, DATE, null, START, END, null, null
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

        assertThatThrownBy(() -> scheduleService.deleteSchedule(99L, USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("일정 삭제 시 해당 ID로 delete가 호출된다")
    void 일정_삭제_성공() {
        given(loadSchedulePort.findByIdAndUserId(1L, USER_ID))
                .willReturn(Optional.of(savedSchedule()));

        scheduleService.deleteSchedule(1L, USER_ID);

        verify(saveSchedulePort).delete(1L);
    }
}
