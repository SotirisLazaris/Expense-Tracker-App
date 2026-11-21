package com.portfolio.expensetracker.dto;

import com.portfolio.expensetracker.model.Budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

public record BudgetResponse(
        Long id,
        Long categoryId,
        String categoryName,
        YearMonth month,
        BigDecimal amount,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentUsed) {

    public static BudgetResponse of(Budget budget, BigDecimal spent) {
        BigDecimal percent = spent.multiply(BigDecimal.valueOf(100))
                .divide(budget.getAmount(), 1, RoundingMode.HALF_UP);
        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                YearMonth.from(budget.getBudgetMonth()),
                budget.getAmount(),
                spent,
                budget.getAmount().subtract(spent),
                percent);
    }
}
