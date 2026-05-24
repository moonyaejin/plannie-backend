package com.plannie.application.service;

import com.plannie.application.port.in.GetStatisticsUseCase;
import com.plannie.application.port.out.LoadCategoryPort;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.domain.schedule.Category;
import com.plannie.domain.schedule.Schedule;
import com.plannie.domain.schedule.ScheduleException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
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

        // 1. 일회성 일정
        List<Schedule> oneTimeSchedules = loadSchedulePort.findOneTimeSchedulesByDateRange(userId, start, end);
        int total = oneTimeSchedules.size();
        int completed = (int) oneTimeSchedules.stream().filter(Schedule::isCompleted).count();
        Map<Long, Long> categoryCounts = new HashMap<>(oneTimeSchedules.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCategoryId() != null ? s.getCategoryId() : -1L,
                        Collectors.counting()
                )));

        // 2. 반복 일정 확장
        List<Schedule> repeatingSchedules = loadSchedulePort.findRepeatingSchedules(userId);
        if (!repeatingSchedules.isEmpty()) {
            List<Long> repeatingIds = repeatingSchedules.stream().map(Schedule::getId).toList();
            Map<String, ScheduleException> exceptions = loadSchedulePort.findExceptions(repeatingIds, start, end);
            Map<String, Boolean> completions = loadSchedulePort.findCompletions(repeatingIds, start, end);

            for (Schedule s : repeatingSchedules) {
                LocalDate current = s.getStartDate().isBefore(start) ? start : s.getStartDate();
                LocalDate repeatEnd = s.getRepeatRule().getEndDate() != null && s.getRepeatRule().getEndDate().isBefore(end)
                        ? s.getRepeatRule().getEndDate() : end;
                for (LocalDate date = current; !date.isAfter(repeatEnd); date = date.plusDays(1)) {
                    if (!s.getRepeatRule().appliesTo(date)) continue;
                    String key = s.getId() + "_" + date;
                    ScheduleException ex = exceptions.get(key);
                    if (ex != null && ex.isDeleted()) continue;
                    total++;
                    if (Boolean.TRUE.equals(completions.get(key))) completed++;
                    Long catKey = s.getCategoryId() != null ? s.getCategoryId() : -1L;
                    categoryCounts.merge(catKey, 1L, Long::sum);
                }
            }
        }

        double completionRate = total == 0 ? 0.0
                : Math.round(completed * 100.0 / total * 10) / 10.0;

        Map<Long, String> categoryNames = loadCategoryPort.findAllByUserIdOrDefault(userId)
                .stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

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
