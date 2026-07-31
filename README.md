# BrainRidge Banking — Transactions API

A simple banking demo app built with **Java 17** and **Spring Boot 3**. Create accounts, transfer money between them, check balances, and view transaction history.

**Live demo UI:** [http://localhost:8080](http://localhost:8080) (after starting the app)

**GitHub:** [github.com/Kabith-Kuber/banking-transactions-api](https://github.com/Kabith-Kuber/banking-transactions-api)

---

## Table of Contents

- [Who is this for?](#who-is-this-for)
- [Quick Start (3 steps)](#quick-start-3-steps)
- [How to Use the App (Full Walkthrough)](#how-to-use-the-app-full-walkthrough)
- [How It Works (Behind the Scenes)](#how-it-works-behind-the-scenes)
- [For Developers (API Reference)](#for-developers-api-reference)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)
- [Assumptions](#assumptions)

---

## Who is this for?

This project was built for the **BrainRidge Consulting take-home assignment**. It has two audiences:

| Audience | What to use |
|----------|-------------|
| **Anyone trying the app** | Open [http://localhost:8080](http://localhost:8080) — no technical knowledge needed |
| **Developers / reviewers** | Read the API section below, or use [Swagger UI](http://localhost:8080/swagger-ui/index.html) |

---

## Quick Start (3 steps)

### 1. Prerequisites

- **Java 17+** installed ([download Java](https://www.oracle.com/java/technologies/downloads/))
- **No Maven install needed** — the project includes `mvnw.cmd`

### 2. Start the app

**Easiest way:** double-click **`run.bat`** in the project folder.

**Or from terminal:**

```bat
.\mvnw.cmd spring-boot:run
```

Wait until you see:

```
Started BankingApplication
```

### 3. Open in your browser

Go to: **http://localhost:8080**

You'll see a friendly dashboard with a **Start walkthrough** button and a **Run quick demo** button.

> **Tip:** Leave the terminal window open while using the app. Closing it stops the server.

---

## How to Use the App (Full Walkthrough)

### First time? Click "Run quick demo"

This automatically:
1. Creates **Alice** with $1,000
2. Creates **Bob** with $500
3. Sends **$150** from Alice to Bob (for "Rent payment")
4. Shows you the updated balances and transaction history

You can watch the **"What just happened"** panel on the right for a plain-English log.

---

### Manual walkthrough (step by step)

#### Step 1 — Create an account

1. Go to **"1 — Create an account"**
2. Enter a name (e.g. `Alice`)
3. Enter a starting balance (e.g. `1000`)
4. Click **Create account**

**What happens:** A new account is opened with that starting balance. The account appears in **"Your accounts"** on the right.

**Do this twice** — you need at least 2 accounts to send money.

---

#### Step 2 — Send money

1. Go to **"2 — Send money"**
2. **From:** pick who is paying (e.g. Alice)
3. **To:** pick who receives (e.g. Bob)
4. **Amount:** e.g. `150`
5. **Note (optional):** e.g. `Rent payment`
6. Click **Send transfer**

**What happens:**
- Money is deducted from Alice → she now has **$850**
- Money is added to Bob → he now has **$650**
- A transaction record is saved

**Rules:**
- You can't send to the same account
- You can't send more than the sender's balance

---

#### Step 3 — Check balance

1. Go to **"3 — Check balance"**
2. Pick an account
3. Click **Check balance**

**What happens:** You see the current balance and when the account was opened.

---

#### Step 4 — View transaction history

1. Go to **"4 — Transaction history"**
2. Pick an account
3. Click **Load history**

**What happens:** You see a table of every transfer that account sent or received.

| Column | Meaning |
|--------|---------|
| **Sent** | This account paid someone |
| **Received** | This account got money from someone |
| **Other person** | Who they sent to or received from |
| **Amount** | How much money moved |
| **Note** | Optional description (e.g. "Rent") |
| **When** | Date and time of the transfer |

---

### Progress tracker

At the top of the page, **"Your progress"** shows which steps you've completed:
1. Create an account
2. Create a second account
3. Send a transfer
4. View history

It also tells you **what to do next**.

---

### Common questions

| Problem | Solution |
|---------|----------|
| "Server offline" at the top | Run `run.bat` and wait for `Started BankingApplication` |
| Dropdowns are empty | Create at least one account first |
| "Account not found" | You restarted the server — data is cleared. Create new accounts. |
| "Insufficient funds" | The sender doesn't have enough money. Lower the amount. |
| Accounts disappeared | Data is in-memory only. Restarting the server wipes everything. |

---

## How It Works (Behind the Scenes)

### What happens when you click a button?

```
Browser (UI)  →  REST API  →  Service Layer  →  In-Memory Storage
```

1. **Browser UI** (`index.html`) — forms and buttons you interact with
2. **REST API** (`/api/v1/...`) — receives requests, validates input, returns JSON
3. **Service Layer** — business rules (e.g. "can't send more than balance")
4. **In-Memory Storage** — accounts and transactions stored in RAM (lost on restart)

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Presentation Layer (Controllers + Web UI)              │
│  AccountController, TransferController, index.html      │
├─────────────────────────────────────────────────────────┤
│  Business Layer (Services)                              │
│  AccountService, TransferService, TransactionService    │
├─────────────────────────────────────────────────────────┤
│  Data Layer (Repositories)                              │
│  InMemoryAccountRepository, InMemoryTransactionRepository│
├─────────────────────────────────────────────────────────┤
│  Storage: ConcurrentHashMap (in RAM)                    │
└─────────────────────────────────────────────────────────┘
```

### Key design decisions

- **BigDecimal** for money — avoids floating-point rounding errors
- **UUID** for account IDs — safe for concurrent creation
- **Ordered locking** on transfers — prevents race conditions when two transfers happen at once
- **DTOs** at the API boundary — internal data structures are never exposed directly
- **No database** — assignment requires in-memory storage only

### What the app stores

**Account:**
- ID, owner name, balance, created date

**Transaction:**
- ID, from account, to account, amount, description, timestamp, type (TRANSFER)

---

## For Developers (API Reference)

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/accounts` | Create account |
| `GET` | `/api/v1/accounts/{id}` | Get account |
| `POST` | `/api/v1/transfers` | Transfer funds |
| `GET` | `/api/v1/accounts/{id}/transactions` | Paginated history |

### Swagger UI

[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

### Example: Create account

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d "{\"ownerName\":\"Jane Doe\",\"initialBalance\":1000.00}"
```

### Example: Transfer

```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\":\"{fromId}\",\"toAccountId\":\"{toId}\",\"amount\":50.00,\"description\":\"Payment\"}"
```

### Build

```bat
.\mvnw.cmd package
```

---

## Running Tests

```bat
.\mvnw.cmd test
```

Includes unit tests (services) and integration tests (full API flows via MockMvc).

---

## Project Structure

```
src/main/java/com/brainridge/banking/
├── controller/       # REST API endpoints
├── service/          # Business logic
├── repository/       # In-memory data access
├── dto/              # Request/response objects
├── model/            # Domain entities (Account, Transaction)
├── exception/        # Error handling
└── config/           # OpenAPI config

src/main/resources/
├── static/           # Web UI (index.html, css, js)
└── application.properties

src/test/java/        # Unit + integration tests
```

---

## Assumptions

1. Data is stored **in memory** and is **lost when the application restarts**
2. **No authentication** or authorization is required
3. Monetary values use `BigDecimal` with **2-decimal precision** (`HALF_UP` rounding)
4. **No overdraft** — transfers fail if the source balance is insufficient
5. Transfers are **immediately committed** (no pending state)
6. **Concurrent transfers** use ordered account locking (sorted UUIDs)
7. Transaction history includes both **sent and received** transfers
8. Pagination defaults: `page=0`, `size=20`, max `size=100`
9. Account IDs are **UUIDs** generated server-side
10. Only **TRANSFER** transaction type is supported

---

## Future Enhancements

- Persistent database (PostgreSQL, H2)
- User authentication
- Separate deposit/withdrawal endpoints
- Docker and CI pipeline
