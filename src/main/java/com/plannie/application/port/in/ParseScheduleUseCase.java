package com.plannie.application.port.in;

import com.plannie.domain.schedule.Schedule;

/**
 * 자연어 일정 파싱 및 생성 유스케이스
 */
public interface ParseScheduleUseCase {

    Schedule parseAndCreate(ParseScheduleCommand command);

    record ParseScheduleCommand(
            Long userId,
            String naturalLanguage
    ) {}
}
