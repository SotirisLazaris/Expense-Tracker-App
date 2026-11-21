package com.portfolio.expensetracker.service;

import com.portfolio.expensetracker.dto.CategoryRequest;
import com.portfolio.expensetracker.dto.CategoryResponse;
import com.portfolio.expensetracker.exception.ConflictException;
import com.portfolio.expensetracker.exception.ResourceNotFoundException;
import com.portfolio.expensetracker.model.Category;
import com.portfolio.expensetracker.model.User;
import com.portfolio.expensetracker.repository.BudgetRepository;
import com.portfolio.expensetracker.repository.CategoryRepository;
import com.portfolio.expensetracker.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private static final List<String> DEFAULT_CATEGORIES =
            List.of("Food", "Transport", "Housing", "Entertainment", "Health", "Other");

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           ExpenseRepository expenseRepository,
                           BudgetRepository budgetRepository) {
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
    }

    /** Gives every new account a sensible starting set of categories. */
    public void createDefaultCategories(User user) {
        categoryRepository.saveAll(DEFAULT_CATEGORIES.stream()
                .map(name -> new Category(user, name))
                .toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(User user) {
        return categoryRepository.findByUserIdOrderByNameAsc(user.getId()).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse create(User user, CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(user.getId(), name)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }
        return CategoryResponse.from(categoryRepository.save(new Category(user, name)));
    }

    public CategoryResponse update(User user, Long id, CategoryRequest request) {
        Category category = findOwned(user, id);
        String name = request.name().trim();
        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(user.getId(), name, id)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }
        category.setName(name);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public void delete(User user, Long id) {
        Category category = findOwned(user, id);
        if (expenseRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category still has expenses; move or delete them first");
        }
        budgetRepository.deleteByCategoryId(id);
        categoryRepository.delete(category);
    }

    private Category findOwned(User user, Long id) {
        return categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
