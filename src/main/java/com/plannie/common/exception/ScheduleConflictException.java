package com.plannie.common.exception;

import com.plannie.domain.schedule.Schedule;
import lombok.Getter;

import java.util.List;

/**
 * 일정 충돌 예외
 * - 충돌하는 기존 일정 정보 포함
 */
@Getter
public class ScheduleConflictException extends BusinessException {

    private final List<Schedule> conflictingSchedules;

    public ScheduleConflictException(List<Schedule> conflictingSchedules) {
        super(ErrorCode.SCHEDULE_CONFLICT);
        this.conflictingSchedules = conflictingSchedules;
    }
}
