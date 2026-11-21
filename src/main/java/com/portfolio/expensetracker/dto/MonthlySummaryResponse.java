package com.portfolio.expensetracker.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record MonthlySummaryResponse(
        YearMonth month,
        BigDecimal total,
        long expenseCount,
        BigDecimal totalBudget,
        List<CategorySummary> byCategory) {
}
