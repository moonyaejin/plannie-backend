package com.plannie.application.service;

import com.plannie.application.port.in.GenerateStudyPlanUseCase.GenerateStudyPlanCommand;
import com.plannie.application.port.in.GenerateStudyPlanUseCase.StudyPlanResult;
import com.plannie.application.port.out.GenerateStudyPlanWithAiPort;
import com.plannie.application.port.out.GenerateStudyPlanWithAiPort.AiScheduleItem;
import com.plannie.application.port.out.GenerateStudyPlanWithAiPort.AiStudyPlan;
import com.plannie.application.port.out.SaveSchedulePort;
import com.plannie.domain.schedule.Schedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Tag("fast")
@ExtendWith(MockitoExtension.class)
class GenerateStudyPlanServiceTest {

    @Mock
    private GenerateStudyPlanWithAiPort generateStudyPlanWithAiPort;

    @Mock
    private SaveSchedulePort saveSchedulePort;

    @InjectMocks
    private GenerateStudyPlanService generateStudyPlanService;

    private final GenerateStudyPlanCommand command = new GenerateStudyPlanCommand(
            1L, "정보처리기사", LocalDate.of(2026, 4, 10), LocalDate.of(2026, 5, 10),
            "수제비", 2, 3, List.of("데이터베이스", "운영체제"), null
    );

    @Test
    @DisplayName("AI가 반환한 일정 수만큼 저장하고 결과를 반환한다")
    void AI_일정_저장_후_반환() {
        AiStudyPlan aiPlan = new AiStudyPlan(
                "30일 정보처리기사 학습 계획",
                List.of("1주차: 데이터베이스 기초"),
                List.of(
                        new AiScheduleItem("데이터베이스 기초", "1장 학습", "2026-04-10", "20:00", "22:00", 1),
                        new AiScheduleItem("운영체제 개요", null, "2026-04-11", "20:00", "22:00", 1)
                )
        );
        given(generateStudyPlanWithAiPort.generate(command)).willReturn(aiPlan);
        given(saveSchedulePort.save(any())).willAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            return Schedule.builder()
                    .id((long) (Math.random() * 1000))
                    .userId(s.getUserId()).title(s.getTitle()).memo(s.getMemo())
                    .startDate(s.getStartDate()).endDate(s.getEndDate())
                    .startTime(s.getStartTime()).endTime(s.getEndTime())
                    .completed(false).repeatRule(s.getRepeatRule()).build();
        });

        StudyPlanResult result = generateStudyPlanService.generate(command);

        assertThat(result.schedules()).hasSize(2);
        assertThat(result.planSummary()).isEqualTo("30일 정보처리기사 학습 계획");
        assertThat(result.weeklyGoals()).containsExactly("1주차: 데이터베이스 기초");
        verify(saveSchedulePort, times(2)).save(any());
    }

    @Test
    @DisplayName("시간이 null이거나 파싱 불가능하면 시간 없이(null) 저장된다")
    void 시간_없으면_null로_저장() {
        AiStudyPlan aiPlan = new AiStudyPlan(
                "계획",
                List.of(),
                List.of(new AiScheduleItem("제목", null, "2026-04-10", null, "invalid", 1))
        );
        given(generateStudyPlanWithAiPort.generate(command)).willReturn(aiPlan);
        given(saveSchedulePort.save(any())).willAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            return Schedule.builder()
                    .id(1L).userId(s.getUserId()).title(s.getTitle())
                    .startDate(s.getStartDate()).endDate(s.getEndDate())
                    .startTime(s.getStartTime()).endTime(s.getEndTime())
                    .completed(false).repeatRule(s.getRepeatRule()).build();
        });

        generateStudyPlanService.generate(command);

        verify(saveSchedulePort).save(argThat(s ->
                s.getStartTime() == null && s.getEndTime() == null
        ));
    }

    @Test
    @DisplayName("날짜 파싱 실패 시 오늘 날짜로 저장된다")
    void 날짜_파싱_실패시_오늘() {
        AiStudyPlan aiPlan = new AiStudyPlan(
                "계획", List.of(),
                List.of(new AiScheduleItem("제목", null, "invalid-date", "20:00", "22:00", 1))
        );
        given(generateStudyPlanWithAiPort.generate(command)).willReturn(aiPlan);
        given(saveSchedulePort.save(any())).willAnswer(inv -> inv.getArgument(0));

        generateStudyPlanService.generate(command);

        verify(saveSchedulePort).save(argThat(s ->
                s.getStartDate().equals(LocalDate.now())
        ));
    }

    private static <T> T argThat(org.mockito.ArgumentMatcher<T> matcher) {
        return org.mockito.Mockito.argThat(matcher);
    }
}
