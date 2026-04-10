package com.plannie.application.port.in;

import java.io.Serializable;
import java.util.List;

public interface GetStatisticsUseCase {

    MonthlyStats getMonthlyStats(Long userId, int year, int month);

    record MonthlyStats(
            int year,
            int month,
            int totalSchedules,
            int completedSchedules,
            double completionRate,
            List<CategoryStat> byCategory
    ) implements Serializable {}

    record CategoryStat(Long categoryId, String categoryName, int count) implements Serializable {}
}
