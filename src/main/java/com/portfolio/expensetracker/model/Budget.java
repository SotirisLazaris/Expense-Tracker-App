package com.portfolio.expensetracker.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A spending limit for one category in one calendar month (stored as the first day of that month). */
@Entity
@Table(name = "budgets",
        uniqueConstraints = @UniqueConstraint(name = "uk_budget_user_category_month",
                columnNames = {"user_id", "category_id", "budget_month"}))
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "budget_month", nullable = false)
    private LocalDate budgetMonth;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    protected Budget() {
        // required by JPA
    }

    public Budget(User user, Category category, LocalDate budgetMonth, BigDecimal amount) {
        this.user = user;
        this.category = category;
        this.budgetMonth = budgetMonth;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Category getCategory() { return category; }
    public LocalDate getBudgetMonth() { return budgetMonth; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
