# Expense Tracker API

A REST API for tracking personal expenses, setting monthly budgets per category and getting spending summaries.
Built with **Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA** and documented with **OpenAPI / Swagger UI**.

<!-- Add a screenshot or GIF of Swagger UI here: docs/swagger.png -->

## Features

- **Web dashboard**: minimal single-page UI at `/` (log in, add and search expenses, category breakdown with budget bars) with light and dark themes
- **Dashboard extras**: pie chart, a distinct color per category, currency switcher (amounts are stored in EUR and converted with ECB rates from the Frankfurter API, cached for 12 hours) and language switcher (English, Greek, Spanish)
- **Authentication**: register / login, stateless JWT, BCrypt password hashing
- **Expenses**: full CRUD, each tied to a category
- **Search**: filter by category, date range, amount range and free text; sort (`date`, `amount`, `description`); pagination
- **Categories**: per-user categories (a default set is created on sign-up); cannot delete a category that still has expenses
- **Budgets**: one budget per category per month, returned together with spent / remaining / percent used
- **Monthly summary**: totals per category, expense count, and over-budget flags
- **Data isolation**: every query is scoped to the authenticated user; other users' data returns `404`
- **Consistent errors**: one JSON error shape for validation (`400`), auth (`401`), conflicts (`409`), not found (`404`) and unexpected failures (`500`)

## Tech stack

| Concern        | Choice                                                   |
|----------------|----------------------------------------------------------|
| Framework      | Spring Boot 3.3 (Web, Validation, Actuator)              |
| Security       | Spring Security 6, JWT via jjwt, BCrypt                  |
| Persistence    | Spring Data JPA / Hibernate, H2 (default) or PostgreSQL  |
| Docs           | springdoc-openapi (Swagger UI)                           |
| Testing        | JUnit 5, Mockito, MockMvc (integration tests on H2)      |
| Packaging      | Maven, multi-stage Dockerfile, docker-compose            |

## Getting started

Requirements: JDK 17+ and Maven 3.9+.

### Run locally (in-memory H2)

```bash
mvn spring-boot:run
```

- Dashboard: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

### Run with PostgreSQL via Docker

```bash
docker compose up --build
```

### Run the tests

```bash
mvn test
```

### Configuration

| Variable               | Default                                  | Purpose                                         |
|------------------------|------------------------------------------|-------------------------------------------------|
| `JWT_SECRET`           | dev-only key (change it!)                | Base64 HMAC key, 32+ bytes: `openssl rand -base64 48` |
| `JWT_EXPIRATION_MS`    | `3600000` (1 hour)                       | Token lifetime                                  |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Origins allowed to call the API from a browser |
| `SPRING_PROFILES_ACTIVE` | (none)                                 | Set to `postgres` to use PostgreSQL             |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | local Postgres defaults      | Database connection for the `postgres` profile  |

## API overview

All endpoints except `/api/auth/**` require `Authorization: Bearer <token>`.

| Method | Path                       | Description                                   |
|--------|----------------------------|-----------------------------------------------|
| POST   | `/api/auth/register`       | Create account, returns a JWT                 |
| POST   | `/api/auth/login`          | Log in, returns a JWT                         |
| GET    | `/api/categories`          | List categories                               |
| POST   | `/api/categories`          | Create category                               |
| PUT    | `/api/categories/{id}`     | Rename category                               |
| DELETE | `/api/categories/{id}`     | Delete category (only if it has no expenses)  |
| GET    | `/api/expenses`            | Search expenses (filters, sort, pagination)   |
| GET    | `/api/expenses/{id}`       | Get one expense                               |
| POST   | `/api/expenses`            | Create expense                                |
| PUT    | `/api/expenses/{id}`       | Replace expense                               |
| DELETE | `/api/expenses/{id}`       | Delete expense                                |
| GET    | `/api/budgets?month=yyyy-MM` | Budgets with spending for a month           |
| POST   | `/api/budgets`             | Set a budget for a category and month         |
| PUT    | `/api/budgets/{id}`        | Change a budget amount                        |
| DELETE | `/api/budgets/{id}`        | Delete a budget                               |
| GET    | `/api/summary/monthly?month=yyyy-MM` | Spending per category + budget status |

Expense search parameters: `categoryId`, `from`, `to` (ISO dates), `minAmount`, `maxAmount`, `search`,
`page` (default 0), `size` (default 20, max 100), `sortBy` (`date` | `amount` | `description`), `direction` (`asc` | `desc`).

### Example session

```bash
# 1. Register (returns a token)
TOKEN=$(curl -s -X POST localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"me@example.com","password":"password123","fullName":"Me"}' | jq -r .token)

# 2. See the default categories
curl -s localhost:8080/api/categories -H "Authorization: Bearer $TOKEN"

# 3. Add an expense (use a real category id from step 2)
curl -s -X POST localhost:8080/api/expenses \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"amount":12.50,"description":"Lunch","date":"2026-11-18","categoryId":1}'

# 4. Search
curl -s "localhost:8080/api/expenses?search=lunch&minAmount=10&sortBy=amount&direction=desc" \
  -H "Authorization: Bearer $TOKEN"

# 5. Set a budget and get the monthly summary
curl -s -X POST localhost:8080/api/budgets \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"categoryId":1,"month":"2026-11","amount":300}'
curl -s "localhost:8080/api/summary/monthly?month=2026-11" -H "Authorization: Bearer $TOKEN"
```

Error responses share one shape:

```json
{
  "timestamp": "2026-11-20T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/expenses",
  "fieldErrors": { "amount": "must be at least 0.01" }
}
```

## Architecture

```
controller  ->  service  ->  repository  ->  database
   |              |
  DTOs        business rules
(records)     + ownership checks
```

- `controller`: thin HTTP layer, request validation (`@Valid`), OpenAPI annotations
- `service`: business rules, transactions, ownership checks (every lookup includes the user id)
- `repository`: Spring Data interfaces; JPA `Specification`s for dynamic search; a JPQL aggregate with an interface projection for summaries
- `security`: JWT service, authentication filter, JSON 401/403 handlers
- `exception`: custom exceptions and a `@RestControllerAdvice` that maps them to HTTP responses
- `dto`: immutable Java records, so entities are never exposed directly

### Design decisions

- **Money is `BigDecimal`** (`NUMERIC(12,2)`), never `double`.
- **Stateless auth**: no sessions, so the API scales horizontally and works easily with a SPA or mobile client.
- **No cross-user leaks**: resources owned by someone else answer `404`, not `403`, so IDs can't be probed.
- **Whitelisted sorting**: clients choose from `date`, `amount`, `description`; they can't sort by arbitrary entity fields.
- **N+1 avoidance**: expense queries fetch the category with an `@EntityGraph`; budgets use one grouped query for spending.
- **`open-in-view` is disabled** so lazy loading can't hide inefficient queries in the web layer.
- **Database-level constraints** (unique email, unique category name per user, one budget per category/month) back up the checks in the service layer.

## Testing

- `ExpenseServiceTest`: unit tests with Mockito (category ownership, validation rules, sort whitelist)
- `ExpenseApiIntegrationTest`: full-stack tests with MockMvc against H2, covering auth, validation errors, the expense lifecycle with filters, user isolation, and budgets

## Ideas for further work

- Flyway migrations instead of `ddl-auto: update`
- Refresh tokens and logout / token revocation
- Recurring expenses and CSV import / export
- Rate limiting on the auth endpoints
- Testcontainers so integration tests run against real PostgreSQL

## What I learned

_Fill this in before publishing: two or three honest sentences about the hardest part and what you would do differently._
