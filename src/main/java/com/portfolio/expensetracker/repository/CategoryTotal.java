package com.portfolio.expensetracker.repository;

import java.math.BigDecimal;

/** Interface projection for the "spending per category" aggregate query. */
public interface CategoryTotal {

    Long getCategoryId();

    String getCategoryName();

    BigDecimal getTotal();

    Long getExpenseCount();
}
