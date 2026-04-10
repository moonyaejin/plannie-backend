package com.plannie.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plannie.adapter.in.web.dto.WeeklyFeedbackRequest;
import com.plannie.application.port.in.GenerateProgressFeedbackUseCase;
import com.plannie.application.port.in.GenerateProgressFeedbackUseCase.WeeklyFeedback;
import com.plannie.application.port.in.GetStatisticsUseCase;
import com.plannie.application.port.in.GetStatisticsUseCase.CategoryStat;
import com.plannie.application.port.in.GetStatisticsUseCase.MonthlyStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("fast")
@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GetStatisticsUseCase getStatisticsUseCase;

    @MockBean
    private GenerateProgressFeedbackUseCase generateProgressFeedbackUseCase;

    @Test
    @DisplayName("GET /api/statistics/monthly - 월별 통계를 반환한다")
    void 월별_통계_조회() throws Exception {
        MonthlyStats stats = new MonthlyStats(
                2026, 4, 10, 7, 70.0,
                List.of(new CategoryStat(null, "미분류", 10))
        );
        given(getStatisticsUseCase.getMonthlyStats(1L, 2026, 4)).willReturn(stats);

        mockMvc.perform(get("/api/statistics/monthly")
                        .header("X-User-Id", 1L)
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(4))
                .andExpect(jsonPath("$.totalSchedules").value(10))
                .andExpect(jsonPath("$.completedSchedules").value(7))
                .andExpect(jsonPath("$.completionRate").value(70.0))
                .andExpect(jsonPath("$.byCategory[0].categoryName").value("미분류"));
    }

    @Test
    @DisplayName("GET /api/statistics/monthly - X-User-Id 헤더 누락 시 400 반환")
    void 월별_통계_헤더_누락() throws Exception {
        mockMvc.perform(get("/api/statistics/monthly")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/statistics/weekly-feedback - AI 주간 피드백을 반환한다")
    void 주간_피드백_조회() throws Exception {
        WeeklyFeedback feedback = new WeeklyFeedback(
                LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 13),
                5, 4, 80.0,
                "이번 주 좋은 성과를 냈습니다.",
                List.of("꾸준한 학습"),
                List.of("주말 활용 부족"),
                "다음 주도 화이팅!"
        );
        given(generateProgressFeedbackUseCase.generateWeeklyFeedback(
                eq(1L), any(LocalDate.class), any(LocalDate.class)
        )).willReturn(feedback);

        WeeklyFeedbackRequest request = new WeeklyFeedbackRequest(
                LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 13)
        );

        mockMvc.perform(post("/api/statistics/weekly-feedback")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionRate").value(80.0))
                .andExpect(jsonPath("$.summary").value("이번 주 좋은 성과를 냈습니다."))
                .andExpect(jsonPath("$.strengths[0]").value("꾸준한 학습"))
                .andExpect(jsonPath("$.nextWeekAdvice").value("다음 주도 화이팅!"));
    }

    @Test
    @DisplayName("POST /api/statistics/weekly-feedback - weekStart 누락 시 400 반환")
    void 주간_피드백_필수값_누락() throws Exception {
        mockMvc.perform(post("/api/statistics/weekly-feedback")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weekEnd\": \"2026-04-13\"}"))
                .andExpect(status().isBadRequest());
    }
}
