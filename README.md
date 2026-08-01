# BrainRidge Banking — Transactions API

Java 17 + Spring Boot 3 demo: create accounts, transfer money, view transaction history.

**Live demo:** [banking-transactions-api.vercel.app](https://banking-transactions-api.vercel.app)  
**GitHub:** [Kabith-Kuber/banking-transactions-api](https://github.com/Kabith-Kuber/banking-transactions-api)

---

## Quick start

**Needs:** Java 17+

```bat
.\mvnw.cmd spring-boot:run
```

Or double-click `run.bat`, then open [http://localhost:8080](http://localhost:8080).

Swagger: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## How to use

1. Click **Run demo** — creates Alice ($1000) + Bob ($500), then sends $150.
2. Or manually:
   - **Accounts** → open at least 2 accounts  
   - **Send Money** → transfer between them  
   - **Transactions** → load history  
   - **Dashboard** → balances, charts, recent activity  

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
Controller → Service → Repository → Storage
```

- **Local:** in-memory `ConcurrentHashMap` (assignment default)
- **Vercel:** shared Upstash Redis so every instance sees the same data
- **BigDecimal** money (2 decimals), **UUID** ids, ordered locking on transfers
- DTOs + validation + JSON error responses

> Live Redis DB is temporary unless claimed:  
> [Claim / keep the Upstash database](https://upstash.com/start-redis/console/fe632a1b-e3f1-49f2-891c-7430d4400e1a)

---

## Assumptions

1. Local data resets when the app restarts  
2. No authentication  
3. No overdraft — insufficient funds → `422`  
4. History includes sent **and** received transfers  
5. Pagination defaults: `page=0`, `size=20` (max `100`)
