package com.plannie.application.service;

import com.plannie.application.port.in.GetStatisticsUseCase.CategoryStat;
import com.plannie.application.port.in.GetStatisticsUseCase.MonthlyStats;
import com.plannie.application.port.out.LoadCategoryPort;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.domain.schedule.Category;
import com.plannie.domain.schedule.RepeatRule;
import com.plannie.domain.schedule.Schedule;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@Tag("fast")
@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private LoadSchedulePort loadSchedulePort;

    @Mock
    private LoadCategoryPort loadCategoryPort;

    @InjectMocks
    private StatisticsService statisticsService;

    private static final Long USER_ID = 1L;

    private Schedule schedule(Long id, String title, boolean completed, Long categoryId) {
        return Schedule.builder()
                .id(id)
                .userId(USER_ID)
                .title(title)
                .startDate(LocalDate.of(2026, 4, 10))
                .endDate(LocalDate.of(2026, 4, 10))
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(22, 0))
                .completed(completed)
                .categoryId(categoryId)
                .repeatRule(RepeatRule.none())
                .build();
    }

    @BeforeEach
    void setUp() {
        given(loadCategoryPort.findAllByUserIdOrDefault(USER_ID)).willReturn(List.of(
                Category.builder().id(1L).userId(USER_ID).name("업무").color("#FF0000").build(),
                Category.builder().id(2L).userId(USER_ID).name("공부").color("#0000FF").build()
        ));
    }

    @Test
    @DisplayName("일정이 없으면 완료율 0%, 카테고리 목록 비어있음")
    void 일정_없을때_통계() {
        given(loadSchedulePort.findOneTimeSchedulesByDateRange(eq(USER_ID), any(), any()))
                .willReturn(List.of());
        given(loadSchedulePort.findRepeatingSchedules(USER_ID)).willReturn(List.of());

        MonthlyStats stats = statisticsService.getMonthlyStats(USER_ID, 2026, 4);

        assertThat(stats.totalSchedules()).isZero();
        assertThat(stats.completedSchedules()).isZero();
        assertThat(stats.completionRate()).isEqualTo(0.0);
        assertThat(stats.byCategory()).isEmpty();
    }

    @Test
    @DisplayName("완료된 일정과 미완료 일정이 섞여 있으면 완료율을 올바르게 계산한다")
    void 완료율_계산() {
        given(loadSchedulePort.findOneTimeSchedulesByDateRange(eq(USER_ID), any(), any()))
                .willReturn(List.of(
                        schedule(1L, "일정A", true, null),
                        schedule(2L, "일정B", true, null),
                        schedule(3L, "일정C", false, null)
                ));
        given(loadSchedulePort.findRepeatingSchedules(USER_ID)).willReturn(List.of());

        MonthlyStats stats = statisticsService.getMonthlyStats(USER_ID, 2026, 4);

        assertThat(stats.totalSchedules()).isEqualTo(3);
        assertThat(stats.completedSchedules()).isEqualTo(2);
        assertThat(stats.completionRate()).isEqualTo(66.7);
    }

    @Test
    @DisplayName("카테고리가 있는 일정은 카테고리명으로, 없는 일정은 미분류로 집계된다")
    void 카테고리별_집계() {
        given(loadSchedulePort.findOneTimeSchedulesByDateRange(eq(USER_ID), any(), any()))
                .willReturn(List.of(
                        schedule(1L, "업무1", false, 1L),
                        schedule(2L, "업무2", false, 1L),
                        schedule(3L, "공부1", false, 2L),
                        schedule(4L, "미분류", false, null)
                ));
        given(loadSchedulePort.findRepeatingSchedules(USER_ID)).willReturn(List.of());

        MonthlyStats stats = statisticsService.getMonthlyStats(USER_ID, 2026, 4);

        assertThat(stats.byCategory()).hasSize(3);

        CategoryStat 업무 = stats.byCategory().stream()
                .filter(c -> "업무".equals(c.categoryName())).findFirst().orElseThrow();
        assertThat(업무.count()).isEqualTo(2);

        CategoryStat 미분류 = stats.byCategory().stream()
                .filter(c -> "미분류".equals(c.categoryName())).findFirst().orElseThrow();
        assertThat(미분류.categoryId()).isNull();
        assertThat(미분류.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("카테고리별 집계는 일정 수 내림차순으로 정렬된다")
    void 카테고리_내림차순_정렬() {
        given(loadSchedulePort.findOneTimeSchedulesByDateRange(eq(USER_ID), any(), any()))
                .willReturn(List.of(
                        schedule(1L, "공부1", false, 2L),
                        schedule(2L, "업무1", false, 1L),
                        schedule(3L, "업무2", false, 1L),
                        schedule(4L, "업무3", false, 1L)
                ));
        given(loadSchedulePort.findRepeatingSchedules(USER_ID)).willReturn(List.of());

        MonthlyStats stats = statisticsService.getMonthlyStats(USER_ID, 2026, 4);

        assertThat(stats.byCategory().get(0).categoryName()).isEqualTo("업무");
        assertThat(stats.byCategory().get(0).count()).isEqualTo(3);
        assertThat(stats.byCategory().get(1).categoryName()).isEqualTo("공부");
    }
}
