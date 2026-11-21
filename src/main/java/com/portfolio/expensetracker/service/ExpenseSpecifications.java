package com.portfolio.expensetracker.service;

import com.portfolio.expensetracker.dto.ExpenseFilter;
import com.portfolio.expensetracker.model.Expense;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Composable query fragments used for the dynamic expense search. */
final class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    static Specification<Expense> forFilter(Long userId, ExpenseFilter filter) {
        Specification<Expense> spec = Specification.where(belongsTo(userId));
        if (filter.categoryId() != null) {
            spec = spec.and(inCategory(filter.categoryId()));
        }
        if (filter.from() != null) {
            spec = spec.and(onOrAfter(filter.from()));
        }
        if (filter.to() != null) {
            spec = spec.and(onOrBefore(filter.to()));
        }
        if (filter.minAmount() != null) {
            spec = spec.and(atLeast(filter.minAmount()));
        }
        if (filter.maxAmount() != null) {
            spec = spec.and(atMost(filter.maxAmount()));
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            spec = spec.and(descriptionContains(filter.search().trim()));
        }
        return spec;
    }

    private static Specification<Expense> belongsTo(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    private static Specification<Expense> inCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    private static Specification<Expense> onOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("expenseDate"), from);
    }

    private static Specification<Expense> onOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("expenseDate"), to);
    }

    private static Specification<Expense> atLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("amount"), min);
    }

    private static Specification<Expense> atMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("amount"), max);
    }

    private static Specification<Expense> descriptionContains(String text) {
        String pattern = "%" + text.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.<String>get("description")), pattern);
    }
}
