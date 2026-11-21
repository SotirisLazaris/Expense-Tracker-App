package com.portfolio.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull @DecimalMin(value = "0.01", message = "must be at least 0.01")
        @Digits(integer = 10, fraction = 2, message = "must have at most 10 integer digits and 2 decimals")
        BigDecimal amount,

        @NotBlank @Size(max = 200) String description,

        @NotNull @PastOrPresent LocalDate date,

        @NotNull Long categoryId,

        @Size(max = 500) String notes) {
}
