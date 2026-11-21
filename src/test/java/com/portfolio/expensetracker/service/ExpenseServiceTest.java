package com.portfolio.expensetracker.service;

import com.portfolio.expensetracker.dto.ExpenseFilter;
import com.portfolio.expensetracker.dto.ExpenseRequest;
import com.portfolio.expensetracker.dto.ExpenseResponse;
import com.portfolio.expensetracker.exception.BadRequestException;
import com.portfolio.expensetracker.exception.ResourceNotFoundException;
import com.portfolio.expensetracker.model.Category;
import com.portfolio.expensetracker.model.Expense;
import com.portfolio.expensetracker.model.User;
import com.portfolio.expensetracker.repository.CategoryRepository;
import com.portfolio.expensetracker.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("alice@example.com", "hashed", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    void create_savesExpenseForUserAndCategory() {
        Category food = new Category(user, "Food");
        ReflectionTestUtils.setField(food, "id", 10L);
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(food));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("12.50"), "  Lunch  ", LocalDate.of(2026, 11, 1), 10L, "   ");

        ExpenseResponse response = expenseService.create(user, request);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getCategory()).isSameAs(food);
        assertThat(saved.getDescription()).isEqualTo("Lunch");
        assertThat(saved.getNotes()).isNull();

        assertThat(response.amount()).isEqualByComparingTo("12.50");
        assertThat(response.category().name()).isEqualTo("Food");
    }

    @Test
    void create_withCategoryOfAnotherUser_throwsNotFound() {
        when(categoryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("5.00"), "Coffee", LocalDate.of(2026, 11, 1), 99L, null);

        assertThatThrownBy(() -> expenseService.create(user, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void delete_unknownExpense_throwsNotFound() {
        when(expenseRepository.findByIdAndUserId(404L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.delete(user, 404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
        verify(expenseRepository, never()).delete(any(Expense.class));
    }

    @Test
    void search_withInvertedDateRange_throwsBadRequest() {
        ExpenseFilter filter = new ExpenseFilter(
                null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1), null, null, null);

        assertThatThrownBy(() -> expenseService.search(user, filter, 0, 20, "date", "desc"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void search_withUnknownSortField_throwsBadRequest() {
        ExpenseFilter filter = new ExpenseFilter(null, null, null, null, null, null);

        assertThatThrownBy(() -> expenseService.search(user, filter, 0, 20, "password", "desc"))
                .isInstanceOf(BadRequestException.class);
    }
}
