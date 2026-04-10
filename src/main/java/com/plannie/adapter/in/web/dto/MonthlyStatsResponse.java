package com.plannie.adapter.in.web.dto;

import com.plannie.application.port.in.GetStatisticsUseCase.CategoryStat;
import com.plannie.application.port.in.GetStatisticsUseCase.MonthlyStats;

import java.util.List;

public record MonthlyStatsResponse(
        int year,
        int month,
        int totalSchedules,
        int completedSchedules,
        double completionRate,
        List<CategoryStatItem> byCategory
) {
    public static MonthlyStatsResponse from(MonthlyStats stats) {
        List<CategoryStatItem> items = stats.byCategory().stream()
                .map(CategoryStatItem::from)
                .toList();
        return new MonthlyStatsResponse(
                stats.year(), stats.month(),
                stats.totalSchedules(), stats.completedSchedules(),
                stats.completionRate(), items
        );
    }

    public record CategoryStatItem(Long categoryId, String categoryName, int count) {
        public static CategoryStatItem from(CategoryStat stat) {
            return new CategoryStatItem(stat.categoryId(), stat.categoryName(), stat.count());
        }
    }
}
