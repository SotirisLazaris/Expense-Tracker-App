package com.portfolio.expensetracker.dto;

import java.math.BigDecimal;

/** Spending for one category in a month, with budget information when a budget exists. */
public record CategorySummary(
        Long categoryId,
        String categoryName,
        BigDecimal total,
        long expenseCount,
        BigDecimal budget,
        BigDecimal remaining,
        boolean overBudget) {

    public static CategorySummary of(Long categoryId, String categoryName, BigDecimal total,
                                     long expenseCount, BigDecimal budget) {
        BigDecimal remaining = budget == null ? null : budget.subtract(total);
        boolean over = budget != null && total.compareTo(budget) > 0;
        return new CategorySummary(categoryId, categoryName, total, expenseCount, budget, remaining, over);
    }
}
