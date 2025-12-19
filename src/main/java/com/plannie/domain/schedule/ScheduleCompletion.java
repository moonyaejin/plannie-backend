package com.plannie.domain.schedule;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class ScheduleCompletion {
    private Long id;
    private Long scheduleId;
    private LocalDate completionDate;
    private boolean completed;
}