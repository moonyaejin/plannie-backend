package com.plannie.application.service;

import com.plannie.application.port.in.ParseScheduleUseCase;
import com.plannie.application.port.out.ParseScheduleWithAiPort;
import com.plannie.application.port.out.SaveSchedulePort;
import com.plannie.domain.schedule.RepeatRule;
import com.plannie.domain.schedule.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ParseScheduleService implements ParseScheduleUseCase {

    private final ParseScheduleWithAiPort parseScheduleWithAiPort;
    private final SaveSchedulePort saveSchedulePort;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Schedule parseAndCreate(ParseScheduleCommand command) {
        ParseScheduleWithAiPort.ParsedScheduleResult parsed =
                parseScheduleWithAiPort.parse(command.naturalLanguage(), LocalDate.now());

        Schedule schedule = Schedule.builder()
                .userId(command.userId())
                .title(parsed.title())
                .memo(parsed.memo())
                .startDate(parsed.startDate())
                .endDate(parsed.endDate())
                .startTime(parsed.startTime() != null ? parsed.startTime() : LocalTime.of(0, 0))
                .endTime(parsed.endTime() != null ? parsed.endTime() : LocalTime.of(23, 59))
                .completed(false)
                .repeatRule(buildRepeatRule(parsed))
                .build();

        return saveSchedulePort.save(schedule);
    }

    private RepeatRule buildRepeatRule(ParseScheduleWithAiPort.ParsedScheduleResult parsed) {
        return switch (parsed.repeatType()) {
            case NONE -> RepeatRule.none();
            case DAILY -> RepeatRule.daily(parsed.repeatEndDate());
            case WEEKLY -> RepeatRule.weekly(parsed.daysOfWeek(), parsed.repeatEndDate());
            case MONTHLY -> RepeatRule.monthly(parsed.dayOfMonth(), parsed.repeatEndDate());
        };
    }
}
