package com.plannie.application.port.out;

import com.plannie.domain.schedule.RepeatRule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * AI 자연어 파싱 포트
 * - OpenAI Adapter가 구현
 */
public interface ParseScheduleWithAiPort {

    ParsedScheduleResult parse(String naturalLanguage, LocalDate today);

    record ParsedScheduleResult(
            String title,
            String memo,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            RepeatRule.RepeatType repeatType,
            Set<DayOfWeek> daysOfWeek,
            Integer dayOfMonth,
            LocalDate repeatEndDate
    ) {}
}
