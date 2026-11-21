package com.portfolio.expensetracker.service;

import com.portfolio.expensetracker.dto.CategorySummary;
import com.portfolio.expensetracker.dto.MonthlySummaryResponse;
import com.portfolio.expensetracker.model.Budget;
import com.portfolio.expensetracker.model.User;
import com.portfolio.expensetracker.repository.BudgetRepository;
import com.portfolio.expensetracker.repository.CategoryTotal;
import com.portfolio.expensetracker.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class SummaryService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    public SummaryService(ExpenseRepository expenseRepository, BudgetRepository budgetRepository) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
    }

    /** Total spending per category for a month, combined with that month's budgets. */
    public MonthlySummaryResponse monthly(User user, YearMonth month) {
        YearMonth target = month != null ? month : YearMonth.now();

        List<CategoryTotal> totals =
                expenseRepository.totalsByCategory(user.getId(), target.atDay(1), target.atEndOfMonth());

        Map<Long, Budget> budgetsByCategory = new LinkedHashMap<>();
        for (Budget budget : budgetRepository.findByUserIdAndBudgetMonth(user.getId(), target.atDay(1))) {
            budgetsByCategory.put(budget.getCategory().getId(), budget);
        }

        List<CategorySummary> rows = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        BigDecimal grandTotal = ZERO;
        long count = 0;

        for (CategoryTotal total : totals) {
            Budget budget = budgetsByCategory.get(total.getCategoryId());
            rows.add(CategorySummary.of(total.getCategoryId(), total.getCategoryName(), total.getTotal(),
                    total.getExpenseCount(), budget == null ? null : budget.getAmount()));
            seen.add(total.getCategoryId());
            grandTotal = grandTotal.add(total.getTotal());
            count += total.getExpenseCount();
        }

        // Categories that have a budget but no spending yet still belong in the summary.
        BigDecimal totalBudget = null;
        for (Budget budget : budgetsByCategory.values()) {
            totalBudget = totalBudget == null ? budget.getAmount() : totalBudget.add(budget.getAmount());
            if (!seen.contains(budget.getCategory().getId())) {
                rows.add(CategorySummary.of(budget.getCategory().getId(), budget.getCategory().getName(),
                        ZERO, 0, budget.getAmount()));
            }
        }

        return new MonthlySummaryResponse(target, grandTotal, count, totalBudget, rows);
    }
}
