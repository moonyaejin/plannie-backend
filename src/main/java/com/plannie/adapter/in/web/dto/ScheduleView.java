package com.plannie.adapter.in.web.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class ScheduleView {
    private Long id;
    private String instanceId;  // "scheduleId_date" 형식
    private String title;
    private String memo;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean completed;
    private Long categoryId;
    private boolean isRecurring;
    private String repeatType;
    private String repeatDays;
}