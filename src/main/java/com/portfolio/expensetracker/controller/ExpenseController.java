package com.portfolio.expensetracker.controller;

import com.portfolio.expensetracker.dto.ExpenseFilter;
import com.portfolio.expensetracker.dto.ExpenseRequest;
import com.portfolio.expensetracker.dto.ExpenseResponse;
import com.portfolio.expensetracker.dto.PageResponse;
import com.portfolio.expensetracker.model.User;
import com.portfolio.expensetracker.service.ExpenseService;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/expenses")
@Tag(name = "Expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    @Operation(summary = "Search your expenses with optional filters, sorting and pagination")
    public PageResponse<ExpenseResponse> search(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        ExpenseFilter filter = new ExpenseFilter(categoryId, from, to, minAmount, maxAmount, search);
        return expenseService.search(user, filter, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one expense")
    public ExpenseResponse get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return expenseService.get(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an expense")
    public ExpenseResponse create(@AuthenticationPrincipal User user,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(user, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an expense")
    public ExpenseResponse update(@AuthenticationPrincipal User user,
                                  @PathVariable Long id,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an expense")
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        expenseService.delete(user, id);
    }
}
