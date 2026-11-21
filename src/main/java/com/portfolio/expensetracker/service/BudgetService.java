package com.portfolio.expensetracker.service;

import com.portfolio.expensetracker.dto.BudgetRequest;
import com.portfolio.expensetracker.dto.BudgetResponse;
import com.portfolio.expensetracker.dto.BudgetUpdateRequest;
import com.portfolio.expensetracker.exception.ConflictException;
import com.portfolio.expensetracker.exception.ResourceNotFoundException;
import com.portfolio.expensetracker.model.Budget;
import com.portfolio.expensetracker.model.Category;
import com.portfolio.expensetracker.model.User;
import com.portfolio.expensetracker.repository.BudgetRepository;
import com.portfolio.expensetracker.repository.CategoryRepository;
import com.portfolio.expensetracker.repository.CategoryTotal;
import com.portfolio.expensetracker.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class BudgetService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         CategoryRepository categoryRepository,
                         ExpenseRepository expenseRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
    }

    /** Lists the budgets of a month (defaults to the current month) together with how much was spent. */
    @Transactional(readOnly = true)
    public List<BudgetResponse> list(User user, YearMonth month) {
        YearMonth target = month != null ? month : YearMonth.now();
        Map<Long, BigDecimal> spent = spentByCategory(user, target);
        return budgetRepository.findByUserIdAndBudgetMonth(user.getId(), target.atDay(1)).stream()
                .sorted(Comparator.comparing(b -> b.getCategory().getName()))
                .map(b -> BudgetResponse.of(b, spent.getOrDefault(b.getCategory().getId(), ZERO)))
                .toList();
    }

    public BudgetResponse create(User user, BudgetRequest request) {
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        LocalDate firstDay = request.month().atDay(1);
        if (budgetRepository.existsByUserIdAndCategoryIdAndBudgetMonth(user.getId(), category.getId(), firstDay)) {
            throw new ConflictException("A budget for this category and month already exists");
        }
        Budget budget = budgetRepository.save(new Budget(user, category, firstDay, request.amount()));
        return toResponse(user, budget);
    }

    public BudgetResponse update(User user, Long id, BudgetUpdateRequest request) {
        Budget budget = findOwned(user, id);
        budget.setAmount(request.amount());
        return toResponse(user, budgetRepository.save(budget));
    }

    public void delete(User user, Long id) {
        budgetRepository.delete(findOwned(user, id));
    }

    // ---- helpers ----

    private Budget findOwned(User user, Long id) {
        return budgetRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
    }

    private BudgetResponse toResponse(User user, Budget budget) {
        Map<Long, BigDecimal> spent = spentByCategory(user, YearMonth.from(budget.getBudgetMonth()));
        return BudgetResponse.of(budget, spent.getOrDefault(budget.getCategory().getId(), ZERO));
    }

    private Map<Long, BigDecimal> spentByCategory(User user, YearMonth month) {
        Map<Long, BigDecimal> result = new HashMap<>();
        for (CategoryTotal total : expenseRepository.totalsByCategory(user.getId(), month.atDay(1), month.atEndOfMonth())) {
            result.put(total.getCategoryId(), total.getTotal());
        }
        return result;
    }
}
