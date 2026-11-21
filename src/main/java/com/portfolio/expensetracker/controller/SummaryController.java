package com.portfolio.expensetracker.controller;

import com.portfolio.expensetracker.dto.MonthlySummaryResponse;
import com.portfolio.expensetracker.model.User;
import com.portfolio.expensetracker.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/summary")
@Tag(name = "Summary")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/monthly")
    @Operation(summary = "Spending per category for a month (yyyy-MM, default: current month), including budgets")
    public MonthlySummaryResponse monthly(@AuthenticationPrincipal User user,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return summaryService.monthly(user, month);
    }
}
