package com.plannie.application.service;

import com.plannie.application.port.in.GenerateWeeklyReportUseCase.WeeklyReport;
import com.plannie.application.port.out.GenerateWeeklyReportWithAiPort;
import com.plannie.application.port.out.GenerateWeeklyReportWithAiPort.AiWeeklyReport;
import com.plannie.application.port.out.GenerateWeeklyReportWithAiPort.ReportRequest;
import com.plannie.application.port.out.LoadCategoryPort;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.application.port.out.StudySessionPort;
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
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@Tag("fast")
@ExtendWith(MockitoExtension.class)
class GenerateWeeklyReportServiceTest {

    @Mock private LoadSchedulePort loadSchedulePort;
    @Mock private StudySessionPort studySessionPort;
    @Mock private LoadCategoryPort loadCategoryPort;
    @Mock private GenerateWeeklyReportWithAiPort reportAiPort;
    @InjectMocks private GenerateWeeklyReportService generateWeeklyReportService;

    private static final Long USER_ID = 1L;
    private static final LocalDate WEEK_START = LocalDate.of(2026, 4, 7);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 4, 13);

    private Schedule schedule(Long id, String title, boolean completed) {
        return Schedule.builder()
                .id(id).userId(USER_ID).title(title)
                .startDate(WEEK_START).endDate(WEEK_START)
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .completed(completed)
                .repeatRule(RepeatRule.none())
                .build();
    }

    private StudySession session(Long id, Long categoryId, int durationMinutes) {
        LocalDateTime start = LocalDateTime.of(WEEK_START, LocalTime.of(9, 0));
        return StudySession.builder()
                .id(id).userId(USER_ID).categoryId(categoryId)
                .startedAt(start)
                .endedAt(start.plusMinutes(durationMinutes))
                .durationMinutes(durationMinutes)
                .build();
    }

    private AiWeeklyReport stubAiReport() {
        return new AiWeeklyReport(
                "이번 주 요약", List.of("잘한 점"), List.of("개선할 점"), "다음 주 조언"
        );
    }

    @Test
    @DisplayName("일정과 공부 시간을 집계하여 AI 분석 결과를 포함한 주간 리포트를 반환한다")
    void 주간_리포트_생성_성공() {
        given(loadSchedulePort.findByUserIdAndDateRange(USER_ID, WEEK_START, WEEK_END))
                .willReturn(List.of(
                        schedule(1L, "수학 공부", true),
                        schedule(2L, "영어 공부", true),
                        schedule(3L, "운동", false)
                ));
        given(studySessionPort.findByUserIdAndDateRange(USER_ID, WEEK_START, WEEK_END))
                .willReturn(List.of(
                        session(1L, 10L, 60),
                        session(2L, 11L, 30)
                ));
        given(loadCategoryPort.findAllByUserIdOrDefault(USER_ID)).willReturn(List.of(
                Category.builder().id(10L).userId(USER_ID).name("수학").color("#FF0000").build(),
                Category.builder().id(11L).userId(USER_ID).name("영어").color("#00FF00").build()
        ));
        given(reportAiPort.generate(any(ReportRequest.class))).willReturn(stubAiReport());

        WeeklyReport report = generateWeeklyReportService.generate(USER_ID, WEEK_START, WEEK_END);

        assertThat(report.totalSchedules()).isEqualTo(3);
        assertThat(report.completedSchedules()).isEqualTo(2);
        assertThat(report.completionRate()).isEqualTo(66.7);
        assertThat(report.totalStudyMinutes()).isEqualTo(90);
        assertThat(report.studyByCategory()).hasSize(2);
        assertThat(report.studyByCategory().get(0).categoryName()).isEqualTo("수학"); // 60분 → 내림차순 1위
        assertThat(report.summary()).isEqualTo("이번 주 요약");
        assertThat(report.strengths()).containsExactly("잘한 점");
        assertThat(report.nextWeekAdvice()).isEqualTo("다음 주 조언");
    }

    @Test
    @DisplayName("일정이 없으면 완료율은 0.0이다")
    void 일정_없을_때_완료율_0() {
        given(loadSchedulePort.findByUserIdAndDateRange(USER_ID, WEEK_START, WEEK_END))
                .willReturn(List.of());
        given(studySessionPort.findByUserIdAndDateRange(USER_ID, WEEK_START, WEEK_END))
                .willReturn(List.of());
        given(loadCategoryPort.findAllByUserIdOrDefault(USER_ID)).willReturn(List.of());
        given(reportAiPort.generate(any(ReportRequest.class))).willReturn(stubAiReport());

        WeeklyReport report = generateWeeklyReportService.generate(USER_ID, WEEK_START, WEEK_END);

        assertThat(report.completionRate()).isEqualTo(0.0);
        assertThat(report.totalStudyMinutes()).isEqualTo(0);
        assertThat(report.studyByCategory()).isEmpty();
    }

    @Test
    @DisplayName("삭제된 카테고리의 세션은 '삭제된 카테고리'로 표시된다")
    void 삭제된_카테고리_세션_처리() {
        given(loadSchedulePort.findByUserIdAndDateRange(USER_ID, WEEK_START, WEEK_END))
                .willReturn(List.of());
        given(studySessionPort.findByUserIdAndDateRange(USER_ID, WEEK_START, WEEK_END))
                .willReturn(List.of(session(1L, 99L, 45))); // 존재하지 않는 categoryId
        given(loadCategoryPort.findAllByUserIdOrDefault(USER_ID)).willReturn(List.of());
        given(reportAiPort.generate(any(ReportRequest.class))).willReturn(stubAiReport());

        WeeklyReport report = generateWeeklyReportService.generate(USER_ID, WEEK_START, WEEK_END);

        assertThat(report.studyByCategory()).hasSize(1);
        assertThat(report.studyByCategory().get(0).categoryName()).isEqualTo("삭제된 카테고리");
        assertThat(report.totalStudyMinutes()).isEqualTo(45);
    }
}
