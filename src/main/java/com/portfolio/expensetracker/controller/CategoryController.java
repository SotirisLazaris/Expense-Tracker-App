package com.portfolio.expensetracker.controller;

import com.portfolio.expensetracker.dto.CategoryRequest;
import com.portfolio.expensetracker.dto.CategoryResponse;
import com.portfolio.expensetracker.model.User;
import com.portfolio.expensetracker.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List your categories")
    public List<CategoryResponse> list(@AuthenticationPrincipal User user) {
        return categoryService.list(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category")
    public CategoryResponse create(@AuthenticationPrincipal User user,
                                   @Valid @RequestBody CategoryRequest request) {
        return categoryService.create(user, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename a category")
    public CategoryResponse update(@AuthenticationPrincipal User user,
                                   @PathVariable Long id,
                                   @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a category (only when it has no expenses)")
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        categoryService.delete(user, id);
    }
}
