package com.portfolio.expensetracker.repository;

import com.portfolio.expensetracker.model.Budget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @EntityGraph(attributePaths = "category")
    List<Budget> findByUserIdAndBudgetMonth(Long userId, LocalDate budgetMonth);

    @EntityGraph(attributePaths = "category")
    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndCategoryIdAndBudgetMonth(Long userId, Long categoryId, LocalDate budgetMonth);

    void deleteByCategoryId(Long categoryId);
}
