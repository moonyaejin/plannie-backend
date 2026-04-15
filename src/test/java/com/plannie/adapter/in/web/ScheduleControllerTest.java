package com.plannie.adapter.in.web;

import com.plannie.application.port.in.CreateScheduleUseCase;
import com.plannie.application.port.in.DeleteScheduleUseCase;
import com.plannie.application.port.in.GetScheduleUseCase;
import com.plannie.application.port.in.ParseScheduleUseCase;
import com.plannie.application.port.in.UpdateScheduleUseCase;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.schedule.RepeatRule;
import com.plannie.domain.schedule.Schedule;
import com.plannie.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("fast")
@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private CreateScheduleUseCase createScheduleUseCase;
    @MockBean private GetScheduleUseCase getScheduleUseCase;
    @MockBean private UpdateScheduleUseCase updateScheduleUseCase;
    @MockBean private DeleteScheduleUseCase deleteScheduleUseCase;
    @MockBean private ParseScheduleUseCase parseScheduleUseCase;
    @MockBean private JwtProvider jwtProvider;

    private static final Long USER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 4, 15);

    private UsernamePasswordAuthenticationToken auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private Schedule schedule(Long id) {
        return Schedule.builder()
                .id(id).userId(USER_ID).title("스프링 공부")
                .startDate(DATE).endDate(DATE)
                .startTime(LocalTime.of(20, 0)).endTime(LocalTime.of(22, 0))
                .completed(false).repeatRule(RepeatRule.none()).build();
    }

    private String requestBody() {
        return """
                {
                  "title": "스프링 공부",
                  "startDate": "2026-04-15",
                  "startTime": "20:00:00",
                  "endTime": "22:00:00"
                }
                """;
    }

    // ==================== 일정 생성 ====================

    @Test
    @DisplayName("POST /api/schedules - 정상 요청 시 201과 생성된 일정을 반환한다")
    void 일정_생성_성공() throws Exception {
        given(createScheduleUseCase.createSchedule(any())).willReturn(schedule(1L));

        mockMvc.perform(post("/api/schedules")
                        .with(authentication(auth(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("스프링 공부"));
    }

    @Test
    @DisplayName("POST /api/schedules - title 누락 시 400을 반환한다")
    void 일정_생성_title_누락() throws Exception {
        mockMvc.perform(post("/api/schedules")
                        .with(authentication(auth(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-04-15",
                                  "startTime": "20:00:00",
                                  "endTime": "22:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ==================== 일정 단건 조회 ====================

    @Test
    @DisplayName("GET /api/schedules/{id} - 존재하는 일정이면 200과 일정을 반환한다")
    void 일정_단건_조회_성공() throws Exception {
        given(getScheduleUseCase.getSchedule(1L, USER_ID)).willReturn(Optional.of(schedule(1L)));

        mockMvc.perform(get("/api/schedules/1")
                        .with(authentication(auth(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("스프링 공부"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    @DisplayName("GET /api/schedules/{id} - 존재하지 않는 일정이면 404를 반환한다")
    void 일정_단건_조회_없음() throws Exception {
        given(getScheduleUseCase.getSchedule(eq(999L), eq(USER_ID))).willReturn(Optional.empty());

        mockMvc.perform(get("/api/schedules/999")
                        .with(authentication(auth(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("S001"));
    }

    // ==================== 일정 삭제 ====================

    @Test
    @DisplayName("DELETE /api/schedules/{id} - 정상 삭제 시 204를 반환한다")
    void 일정_삭제_성공() throws Exception {
        doNothing().when(deleteScheduleUseCase).deleteSchedule(1L, USER_ID);

        mockMvc.perform(delete("/api/schedules/1")
                        .with(authentication(auth(USER_ID)))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ==================== 완료 토글 ====================

    @Test
    @DisplayName("PATCH /api/schedules/{id}/toggle - 완료 토글 성공 시 200을 반환한다")
    void 완료_토글_성공() throws Exception {
        doNothing().when(updateScheduleUseCase).toggleComplete(1L, USER_ID);

        mockMvc.perform(patch("/api/schedules/1/toggle")
                        .with(authentication(auth(USER_ID)))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/schedules/{id}/toggle - 없는 일정이면 404를 반환한다")
    void 완료_토글_없는_일정() throws Exception {
        doThrow(new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND))
                .when(updateScheduleUseCase).toggleComplete(eq(999L), eq(USER_ID));

        mockMvc.perform(patch("/api/schedules/999/toggle")
                        .with(authentication(auth(USER_ID)))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("S001"));
    }
}
