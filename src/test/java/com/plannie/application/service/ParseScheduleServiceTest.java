package com.plannie.application.service;

import com.plannie.application.port.in.ParseScheduleUseCase.ParseScheduleCommand;
import com.plannie.application.port.out.ParseScheduleWithAiPort;
import com.plannie.application.port.out.ParseScheduleWithAiPort.ParsedScheduleResult;
import com.plannie.application.port.out.SaveSchedulePort;
import com.plannie.domain.schedule.RepeatRule;
import com.plannie.domain.schedule.RepeatRule.RepeatType;
import com.plannie.domain.schedule.Schedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Tag("fast")
@ExtendWith(MockitoExtension.class)
class ParseScheduleServiceTest {

    @Mock
    private ParseScheduleWithAiPort parseScheduleWithAiPort;

    @Mock
    private SaveSchedulePort saveSchedulePort;

    @InjectMocks
    private ParseScheduleService parseScheduleService;

    private static final Long USER_ID = 42L;
    private static final LocalDate DATE = LocalDate.of(2026, 4, 15);
    private static final LocalTime START = LocalTime.of(19, 0);
    private static final LocalTime END = LocalTime.of(21, 0);

    private ParsedScheduleResult parsed(RepeatType repeatType, Set<DayOfWeek> days, Integer dayOfMonth, LocalDate repeatEnd) {
        return new ParsedScheduleResult(
                "헬스장 운동", null, DATE, DATE, START, END,
                repeatType, days, dayOfMonth, repeatEnd
        );
    }

    @Test
    @DisplayName("일회성 일정: AI 파싱 결과를 그대로 저장하고 반환한다")
    void 일회성_일정_파싱_저장() {
        ParseScheduleCommand command = new ParseScheduleCommand(USER_ID, "내일 저녁 7시 헬스장");
        given(parseScheduleWithAiPort.parse(eq("내일 저녁 7시 헬스장"), any())).willReturn(
                parsed(RepeatType.NONE, null, null, null)
        );
        given(saveSchedulePort.save(any())).willAnswer(inv -> inv.getArgument(0));

        Schedule result = parseScheduleService.parseAndCreate(command);

        assertThat(result.getTitle()).isEqualTo("헬스장 운동");
        assertThat(result.getStartDate()).isEqualTo(DATE);
        assertThat(result.getRepeatRule().getType()).isEqualTo(RepeatType.NONE);
        verify(saveSchedulePort).save(any());
    }

    @Test
    @DisplayName("command의 userId가 저장되는 Schedule에 그대로 담긴다")
    void userId는_command에서_가져온다() {
        ParseScheduleCommand command = new ParseScheduleCommand(USER_ID, "매일 아침 러닝");
        given(parseScheduleWithAiPort.parse(any(), any())).willReturn(
                parsed(RepeatType.NONE, null, null, null)
        );
        given(saveSchedulePort.save(any())).willAnswer(inv -> inv.getArgument(0));

        Schedule result = parseScheduleService.parseAndCreate(command);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("WEEKLY 반복: 요일 세트와 종료일을 포함한 RepeatRule이 생성된다")
    void 주간_반복_일정_규칙_생성() {
        ParseScheduleCommand command = new ParseScheduleCommand(USER_ID, "매주 월수금 운동");
        Set<DayOfWeek> days = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        LocalDate repeatEnd = LocalDate.of(2026, 6, 30);
        given(parseScheduleWithAiPort.parse(any(), any())).willReturn(
                parsed(RepeatType.WEEKLY, days, null, repeatEnd)
        );

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        given(saveSchedulePort.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

        parseScheduleService.parseAndCreate(command);

        RepeatRule rule = captor.getValue().getRepeatRule();
        assertThat(rule.getType()).isEqualTo(RepeatType.WEEKLY);
        assertThat(rule.getDaysOfWeek()).containsExactlyInAnyOrder(
                DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY
        );
        assertThat(rule.getEndDate()).isEqualTo(repeatEnd);
    }

    @Test
    @DisplayName("DAILY 반복: 종료일을 포함한 RepeatRule이 생성된다")
    void 일간_반복_일정_규칙_생성() {
        ParseScheduleCommand command = new ParseScheduleCommand(USER_ID, "매일 독서 30분");
        LocalDate repeatEnd = LocalDate.of(2026, 5, 31);
        given(parseScheduleWithAiPort.parse(any(), any())).willReturn(
                parsed(RepeatType.DAILY, null, null, repeatEnd)
        );

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        given(saveSchedulePort.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

        parseScheduleService.parseAndCreate(command);

        RepeatRule rule = captor.getValue().getRepeatRule();
        assertThat(rule.getType()).isEqualTo(RepeatType.DAILY);
        assertThat(rule.getEndDate()).isEqualTo(repeatEnd);
    }
}
