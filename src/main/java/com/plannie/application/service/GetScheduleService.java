package com.plannie.application.service;

import com.plannie.application.port.in.GetScheduleUseCase;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.schedule.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    public List<Schedule> getSchedulesByMonth(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return loadSchedulePort.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    @Override
    public List<Schedule> getSchedulesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return loadSchedulePort.findByUserIdAndDateRange(userId, startDate, endDate);
    }
}
