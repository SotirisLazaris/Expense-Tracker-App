package com.portfolio.expensetracker.dto;

import com.portfolio.expensetracker.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        BigDecimal amount,
        String description,
        LocalDate date,
        String notes,
        CategoryResponse category) {

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getNotes(),
                CategoryResponse.from(expense.getCategory()));
    }
}
