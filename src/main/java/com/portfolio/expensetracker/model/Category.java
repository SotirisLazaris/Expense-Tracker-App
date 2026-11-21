package com.portfolio.expensetracker.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_category_user_name", columnNames = {"user_id", "name"}))
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    protected Category() {
        // required by JPA
    }

    public Category(User user, String name) {
        this.user = user;
        this.name = name;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
