package com.plannie.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 일정 생성/수정 요청 DTO
 * 
 * record 사용 이유:
 * - 불변 객체 (setter 없음)
 * - equals, hashCode, toString 자동 생성
 * - 간결한 문법
 * 
 * 유효성 검증:
 * - @NotBlank: null, "", " " 모두 불허
 * - @NotNull: null만 불허
 * - @Size: 길이 제한
 */
public record ScheduleRequest(
        
        @NotBlank(message = "일정 제목은 필수입니다")
        @Size(max = 20, message = "일정 제목은 20자 이하여야 합니다")
        String title,

        @Size(max = 200, message = "메모는 200자 이하여야 합니다")
        String memo,

        @NotNull(message = "시작 날짜는 필수입니다")
        LocalDate startDate,

        LocalDate endDate,  // null이면 startDate와 동일하게 처리

        @Schema(type = "string", pattern = "HH:mm:ss", example = "14:00:00")
        @JsonFormat(pattern = "HH:mm:ss")
        @NotNull(message = "시작 시간은 필수입니다")
        LocalTime startTime,

        @Schema(type = "string", pattern = "HH:mm:ss", example = "14:00:00")
        @JsonFormat(pattern = "HH:mm:ss")
        @NotNull(message = "종료 시간은 필수입니다")
        LocalTime endTime,

        Long categoryId,  // null 가능 (기본 카테고리 사용)

        String repeatType,  // "NONE", "DAILY", "WEEKLY", "MONTHLY"

        String repeatDays,  // "MON,TUE,WED" (WEEKLY일 때)

        LocalDate repeatEndDate, // 반복 종료일 (null이면 무한 반복)

        Integer reminderMinutes  // null = 알림 없음, 5/10/30 = X분 전 알림
) {
}
