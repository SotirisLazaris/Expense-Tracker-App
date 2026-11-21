package com.portfolio.expensetracker.controller;

import com.portfolio.expensetracker.dto.BudgetRequest;
import com.portfolio.expensetracker.dto.BudgetResponse;
import com.portfolio.expensetracker.dto.BudgetUpdateRequest;
import com.portfolio.expensetracker.model.User;
import com.portfolio.expensetracker.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@Tag(name = "Budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    @Operation(summary = "List budgets for a month (yyyy-MM, default: current month) with spending so far")
    public List<BudgetResponse> list(@AuthenticationPrincipal User user,
                                     @RequestParam(required = false)
                                     @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return budgetService.list(user, month);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Set a monthly budget for a category")
    public BudgetResponse create(@AuthenticationPrincipal User user,
                                 @Valid @RequestBody BudgetRequest request) {
        return budgetService.create(user, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Change the amount of a budget")
    public BudgetResponse update(@AuthenticationPrincipal User user,
                                 @PathVariable Long id,
                                 @Valid @RequestBody BudgetUpdateRequest request) {
        return budgetService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a budget")
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        budgetService.delete(user, id);
    }
}
