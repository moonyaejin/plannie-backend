package com.plannie.application.service;

import com.plannie.application.port.in.GetStatisticsUseCase;
import com.plannie.application.port.out.LoadCategoryPort;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.domain.schedule.Category;
import com.plannie.domain.schedule.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService implements GetStatisticsUseCase {

    private final LoadSchedulePort loadSchedulePort;
    private final LoadCategoryPort loadCategoryPort;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "statistics:monthly", key = "#userId + ':' + #year + ':' + #month")
    public MonthlyStats getMonthlyStats(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<Schedule> schedules = loadSchedulePort.findByUserIdAndDateRange(userId, start, end);

        int total = schedules.size();
        int completed = (int) schedules.stream().filter(Schedule::isCompleted).count();
        double completionRate = total == 0 ? 0.0
                : Math.round(completed * 100.0 / total * 10) / 10.0;

        Map<Long, String> categoryNames = loadCategoryPort.findAllByUserIdOrDefault(userId)
                .stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        Map<Long, Long> categoryCounts = schedules.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCategoryId() != null ? s.getCategoryId() : -1L,
                        Collectors.counting()
                ));

        List<CategoryStat> byCategory = categoryCounts.entrySet().stream()
                .map(e -> {
                    Long catId = e.getKey() == -1L ? null : e.getKey();
                    String name = catId == null ? "미분류" : categoryNames.getOrDefault(catId, "미분류");
                    return new CategoryStat(catId, name, e.getValue().intValue());
                })
                .sorted(Comparator.comparingInt(CategoryStat::count).reversed())
                .toList();

        return new MonthlyStats(year, month, total, completed, completionRate, byCategory);
    }
}
