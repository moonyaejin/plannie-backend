package com.plannie.domain.schedule;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class ScheduleException {
    private Long id;
    private Long scheduleId;
    private LocalDate exceptionDate;
    private ExceptionType exceptionType;
    private String modifiedTitle;
    private String modifiedMemo;
    private LocalTime modifiedStartTime;
    private LocalTime modifiedEndTime;

    public boolean isDeleted() {
        return exceptionType == ExceptionType.DELETED;
    }

    public boolean isModified() {
        return exceptionType == ExceptionType.MODIFIED;
    }

    public enum ExceptionType {
        DELETED,
        MODIFIED
    }
}