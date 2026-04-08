package com.plannie.application.service;

import com.plannie.adapter.in.web.dto.ScheduleView;
import com.plannie.application.port.in.GetScheduleUseCase;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.schedule.RepeatRule;
import com.plannie.domain.schedule.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetScheduleService implements GetScheduleUseCase {

    private final LoadSchedulePort loadSchedulePort;

    @Override
    public Optional<Schedule> getSchedule(Long scheduleId, Long userId) {
        return Optional.of(
                loadSchedulePort.findByIdAndUserId(scheduleId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND))
        );
    }

    @Override
    public List<Schedule> getSchedulesByDate(Long userId, LocalDate date) {
        return loadSchedulePort.findByUserIdAndDate(userId, date);
    }

    @Override
    public List<ScheduleView> getSchedulesByMonth(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return loadSchedulePort.findByUserIdAndDateRange(userId, startDate, endDate)
                .stream().map(this::toView).toList();
    }

    @Override
    public List<ScheduleView> getSchedulesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return loadSchedulePort.findByUserIdAndDateRange(userId, startDate, endDate)
                .stream().map(this::toView).toList();
    }

    private ScheduleView toView(Schedule schedule) {
        RepeatRule rule = schedule.getRepeatRule();
        boolean isRecurring = rule != null && rule.isRepeating();

        String repeatDays = null;
        if (rule != null && rule.getDaysOfWeek() != null) {
            repeatDays = rule.getDaysOfWeek().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","));
        }

        return ScheduleView.builder()
                .id(schedule.getId())
                .instanceId(schedule.getId() + "_" + schedule.getStartDate())
                .title(schedule.getTitle())
                .memo(schedule.getMemo())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .completed(schedule.isCompleted())
                .categoryId(schedule.getCategoryId())
                .isRecurring(isRecurring)
                .repeatType(rule != null ? rule.getType().name() : RepeatRule.RepeatType.NONE.name())
                .repeatDays(repeatDays)
                .build();
    }
}
