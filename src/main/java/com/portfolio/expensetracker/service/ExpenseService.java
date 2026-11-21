package com.portfolio.expensetracker.service;

import com.portfolio.expensetracker.dto.ExpenseFilter;
import com.portfolio.expensetracker.dto.ExpenseRequest;
import com.portfolio.expensetracker.dto.ExpenseResponse;
import com.portfolio.expensetracker.dto.PageResponse;
import com.portfolio.expensetracker.exception.BadRequestException;
import com.portfolio.expensetracker.exception.ResourceNotFoundException;
import com.portfolio.expensetracker.model.Category;
import com.portfolio.expensetracker.model.Expense;
import com.portfolio.expensetracker.model.User;
import com.portfolio.expensetracker.repository.CategoryRepository;
import com.portfolio.expensetracker.repository.ExpenseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class ExpenseService {

    static final int MAX_PAGE_SIZE = 100;

    /** Public sort keys mapped to entity properties, so clients can never sort by arbitrary fields. */
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "date", "expenseDate",
            "amount", "amount",
            "description", "description");

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;

    public ExpenseService(ExpenseRepository expenseRepository, CategoryRepository categoryRepository) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
    }

    public ExpenseResponse create(User user, ExpenseRequest request) {
        Category category = findCategory(user, request.categoryId());
        Expense expense = new Expense(user, category, request.amount(), request.description().trim(),
                request.date(), normalizeNotes(request.notes()));
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(User user, Long id) {
        return ExpenseResponse.from(findOwned(user, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> search(User user, ExpenseFilter filter,
                                                int page, int size, String sortBy, String direction) {
        validateFilter(filter);
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        Page<Expense> result = expenseRepository.findAll(
                ExpenseSpecifications.forFilter(user.getId(), filter), pageable);
        return PageResponse.from(result.map(ExpenseResponse::from));
    }

    public ExpenseResponse update(User user, Long id, ExpenseRequest request) {
        Expense expense = findOwned(user, id);
        expense.setCategory(findCategory(user, request.categoryId()));
        expense.setAmount(request.amount());
        expense.setDescription(request.description().trim());
        expense.setExpenseDate(request.date());
        expense.setNotes(normalizeNotes(request.notes()));
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    public void delete(User user, Long id) {
        expenseRepository.delete(findOwned(user, id));
    }

    // ---- helpers ----

    private Expense findOwned(User user, Long id) {
        return expenseRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    private Category findCategory(User user, Long categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    private static String normalizeNotes(String notes) {
        return (notes == null || notes.isBlank()) ? null : notes.trim();
    }

    private static void validateFilter(ExpenseFilter filter) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new BadRequestException("'from' must not be after 'to'");
        }
        if (filter.minAmount() != null && filter.maxAmount() != null
                && filter.minAmount().compareTo(filter.maxAmount()) > 0) {
            throw new BadRequestException("'minAmount' must not be greater than 'maxAmount'");
        }
    }

    private static Pageable buildPageable(int page, int size, String sortBy, String direction) {
        String property = SORTABLE_FIELDS.get(sortBy);
        if (property == null) {
            throw new BadRequestException("sortBy must be one of: " + String.join(", ", SORTABLE_FIELDS.keySet()));
        }
        Sort.Direction dir;
        if ("asc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.DESC;
        } else {
            throw new BadRequestException("direction must be 'asc' or 'desc'");
        }
        // Secondary sort on id keeps pagination stable when many rows share the same value.
        Sort sort = Sort.by(dir, property).and(Sort.by(Sort.Direction.DESC, "id"));
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
    }
}
