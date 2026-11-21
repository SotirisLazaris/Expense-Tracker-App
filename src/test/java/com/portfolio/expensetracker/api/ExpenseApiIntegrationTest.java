package com.portfolio.expensetracker.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end tests: real Spring context, real security filter chain, in-memory H2 database. */
@SpringBootTest
@AutoConfigureMockMvc
class ExpenseApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------------------------------------------------------- auth

    @Test
    void protectedEndpoint_withoutToken_returns401Json() throws Exception {
        mvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        mvc.perform(get("/api/expenses").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withInvalidPayload_returns400WithFieldErrors() throws Exception {
        String body = json(Map.of("email", "not-an-email", "password", "short", "fullName", "Bob"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void register_twiceWithSameEmail_returns409() throws Exception {
        String email = uniqueEmail();
        register(email);

        String body = json(Map.of("email", email, "password", "password123", "fullName", "Again"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void login_withWrongPassword_returns401_andWithRightPassword_returnsToken() throws Exception {
        String email = uniqueEmail();
        register(email);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email.toUpperCase(), "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    // ---------------------------------------------------------------- expenses

    @Test
    void expenseLifecycle_createReadFilterUpdateSummaryDelete() throws Exception {
        String token = register(uniqueEmail());
        long foodId = firstCategoryId(token);
        String today = LocalDate.now().toString();

        // create
        MvcResult created = mvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("12.50", "Lunch", today, foodId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.category.id").value(foodId))
                .andReturn();
        long expenseId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // read
        mvc.perform(get("/api/expenses/" + expenseId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(12.5));

        // filter + pagination
        mvc.perform(get("/api/expenses")
                        .header("Authorization", bearer(token))
                        .param("search", "lunch")
                        .param("minAmount", "10")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Lunch"));

        mvc.perform(get("/api/expenses")
                        .header("Authorization", bearer(token))
                        .param("minAmount", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // update
        mvc.perform(put("/api/expenses/" + expenseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("20.00", "Team lunch", today, foodId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Team lunch"));

        // monthly summary
        mvc.perform(get("/api/summary/monthly")
                        .header("Authorization", bearer(token))
                        .param("month", YearMonth.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(20.0))
                .andExpect(jsonPath("$.expenseCount").value(1))
                .andExpect(jsonPath("$.byCategory[0].categoryId").value(foodId));

        // delete
        mvc.perform(delete("/api/expenses/" + expenseId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/expenses/" + expenseId).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createExpense_withInvalidPayload_returns400() throws Exception {
        String token = register(uniqueEmail());
        long categoryId = firstCategoryId(token);

        mvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("-5", "", LocalDate.now().plusDays(3).toString(), categoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists())
                .andExpect(jsonPath("$.fieldErrors.description").exists())
                .andExpect(jsonPath("$.fieldErrors.date").exists());
    }

    @Test
    void users_cannotSeeOrModifyEachOthersData() throws Exception {
        String alice = register(uniqueEmail());
        String bob = register(uniqueEmail());

        MvcResult created = mvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("30.00", "Alice's secret", LocalDate.now().toString(),
                                firstCategoryId(alice))))
                .andExpect(status().isCreated())
                .andReturn();
        long aliceExpenseId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/expenses/" + aliceExpenseId).header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/expenses/" + aliceExpenseId).header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/expenses").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listExpenses_withUnknownSortField_returns400() throws Exception {
        String token = register(uniqueEmail());

        mvc.perform(get("/api/expenses").header("Authorization", bearer(token)).param("sortBy", "password"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- categories & budgets

    @Test
    void deleteCategory_withExpenses_returns409() throws Exception {
        String token = register(uniqueEmail());
        long categoryId = firstCategoryId(token);

        mvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("9.99", "Something", LocalDate.now().toString(), categoryId)))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/categories/" + categoryId).header("Authorization", bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void budget_tracksSpendingAgainstLimit() throws Exception {
        String token = register(uniqueEmail());
        long categoryId = firstCategoryId(token);
        String month = YearMonth.now().toString();

        mvc.perform(post("/api/budgets")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("categoryId", categoryId, "month", month, "amount", 100))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spent").value(0.0));

        mvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("40.00", "Groceries", LocalDate.now().toString(), categoryId)))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/budgets").header("Authorization", bearer(token)).param("month", month))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].spent").value(40.0))
                .andExpect(jsonPath("$[0].remaining").value(60.0))
                .andExpect(jsonPath("$[0].percentUsed").value(40.0));

        // a second budget for the same category and month is rejected
        mvc.perform(post("/api/budgets")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("categoryId", categoryId, "month", month, "amount", 50))))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- helpers

    private String register(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "password123", "fullName", "Test User"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long firstCategoryId(String token) throws Exception {
        MvcResult result = mvc.perform(get("/api/categories").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode categories = objectMapper.readTree(result.getResponse().getContentAsString());
        return categories.get(0).get("id").asLong();
    }

    private String expenseJson(String amount, String description, String date, long categoryId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "amount", new java.math.BigDecimal(amount),
                "description", description,
                "date", date,
                "categoryId", categoryId));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
