# Banking Transactions API

A RESTful banking transactions API built with **Java 17** and **Spring Boot 3**. It supports account creation, fund transfers, and paginated transaction history using in-memory storage.

## Prerequisites

- Java 17 or later
- Maven 3.8 or later

## Build

```bash
mvn clean package
```

## Run

```bash
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/accounts` | Create a new account |
| `GET` | `/api/v1/accounts/{accountId}` | Get account details |
| `POST` | `/api/v1/transfers` | Transfer funds between accounts |
| `GET` | `/api/v1/accounts/{accountId}/transactions` | Get paginated transaction history |

### Swagger UI

Interactive API docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Example Requests

### Create an account

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d "{\"ownerName\":\"Jane Doe\",\"initialBalance\":1000.00}"
```

### Get an account

```bash
curl http://localhost:8080/api/v1/accounts/{accountId}
```

### Transfer funds

```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\":\"{fromId}\",\"toAccountId\":\"{toId}\",\"amount\":50.00,\"description\":\"Payment\"}"
```

### Get transaction history

```bash
curl "http://localhost:8080/api/v1/accounts/{accountId}/transactions?page=0&size=20"
```

## Project Structure

```
src/main/java/com/brainridge/banking/
├── controller/     # REST endpoints
├── service/        # Business logic
├── repository/     # In-memory data access
├── dto/            # Request/response objects
├── model/          # Domain entities
├── exception/      # Error handling
└── config/         # OpenAPI configuration
```

## Assumptions

1. Data is stored in memory and is lost when the application restarts.
2. No authentication or authorization is required.
3. Monetary values use `BigDecimal` with 2-decimal precision (`HALF_UP` rounding).
4. Overdrafts are not allowed; transfers fail when the source balance is insufficient.
5. Transfers are committed immediately (no pending state).
6. Concurrent transfers are handled with ordered account locking (sorted UUIDs).
7. Transaction history includes both sent and received transfers for an account.
8. Pagination defaults: `page=0`, `size=20`, maximum `size=100`.
9. Account IDs are UUIDs generated server-side on creation.
10. Only `TRANSFER` transactions are supported.

## Testing

```bash
mvn test
```

## Future Enhancements

- Persistent database storage
- Authentication and authorization
- Separate deposit and withdrawal endpoints
- Docker and CI pipeline support
