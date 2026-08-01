# BrainRidge Banking — Transactions API

Java 17 + Spring Boot 3 demo for creating accounts, transferring money, and viewing transaction history.

**Live demo:** [banking-transactions-api.vercel.app](https://banking-transactions-api.vercel.app)  
**GitHub:** [Kabith-Kuber/banking-transactions-api](https://github.com/Kabith-Kuber/banking-transactions-api)

> Data is **in-memory** (lost on restart). For the most reliable walkthrough, run locally.

---

## Quick start

**Needs:** Java 17+

```bat
.\mvnw.cmd spring-boot:run
```

Or double-click `run.bat`, then open [http://localhost:8080](http://localhost:8080).

Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## How to use

1. Click **Run demo** — creates Alice ($1000) and Bob ($500), then sends $150.
2. Or do it manually:
   - **Accounts** → open at least 2 accounts
   - **Send Money** → transfer between them
   - **Transactions** → load history for an account
   - **Dashboard** → balances, charts, and recent activity

---

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/accounts` | Create account |
| `GET` | `/api/v1/accounts` | List accounts |
| `GET` | `/api/v1/accounts/{id}` | Get account |
| `POST` | `/api/v1/transfers` | Transfer funds |
| `GET` | `/api/v1/accounts/{id}/transactions` | Paginated history |

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d "{\"ownerName\":\"Jane Doe\",\"initialBalance\":1000.00}"
```

---

## Tests

```bat
.\mvnw.cmd test
```

---

## Architecture

```
Controller → Service → In-memory Repository (ConcurrentHashMap)
```

- **BigDecimal** for money (2 decimal places)
- **UUID** account IDs
- Ordered locking on transfers (safe under concurrency)
- DTOs at the API boundary; validation + JSON error responses

---

## Assumptions

1. In-memory storage only — data resets when the app restarts  
2. No authentication  
3. No overdraft — insufficient funds → `422`  
4. History includes both sent and received transfers  
5. Pagination defaults: `page=0`, `size=20` (max `100`)
