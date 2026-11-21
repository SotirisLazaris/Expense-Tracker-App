package com.portfolio.expensetracker.repository;

import com.portfolio.expensetracker.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    /** Fetch the category together with the page of expenses to avoid N+1 queries. */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Expense> findAll(Specification<Expense> spec, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            select c.id as categoryId, c.name as categoryName,
                   sum(e.amount) as total, count(e) as expenseCount
            from Expense e join e.category c
            where e.user.id = :userId
              and e.expenseDate between :rangeStart and :rangeEnd
            group by c.id, c.name
            order by sum(e.amount) desc
            """)
    List<CategoryTotal> totalsByCategory(@Param("userId") Long userId,
                                         @Param("rangeStart") LocalDate rangeStart,
                                         @Param("rangeEnd") LocalDate rangeEnd);
}
