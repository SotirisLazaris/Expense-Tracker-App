package com.portfolio.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;

/** "month" is serialized as yyyy-MM, e.g. 2026-11. */
public record BudgetRequest(
        @NotNull Long categoryId,
        @NotNull YearMonth month,
        @NotNull @DecimalMin(value = "0.01", message = "must be at least 0.01")
        @Digits(integer = 10, fraction = 2) BigDecimal amount) {
}
