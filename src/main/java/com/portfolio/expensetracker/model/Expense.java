package com.portfolio.expensetracker.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "expenses",
        indexes = @Index(name = "idx_expense_user_date", columnList = "user_id, expense_date"))
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Expense() {
        // required by JPA
    }

    public Expense(User user, Category category, BigDecimal amount, String description,
                   LocalDate expenseDate, String notes) {
        this.user = user;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.expenseDate = expenseDate;
        this.notes = notes;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Category getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setCategory(Category category) { this.category = category; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDescription(String description) { this.description = description; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public void setNotes(String notes) { this.notes = notes; }
}
