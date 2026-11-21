package com.portfolio.expensetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Optional filters for searching expenses. Null means "don't filter on this". */
public record ExpenseFilter(
        Long categoryId,
        LocalDate from,
        LocalDate to,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String search) {
}
