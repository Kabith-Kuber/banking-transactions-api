package com.brainridge.banking.controller;

import com.brainridge.banking.dto.request.CreateAccountRequest;
import com.brainridge.banking.dto.response.AccountResponse;
import com.brainridge.banking.dto.response.PageResponse;
import com.brainridge.banking.dto.response.TransactionResponse;
import com.brainridge.banking.service.AccountService;
import com.brainridge.banking.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * HTTP entry point for everything under {@code /api/v1/accounts} (the "Controller" layer).
 *
 * <p>A controller's job is deliberately small: translate an HTTP request into a
 * service call and return the result. All real logic lives in the services, so
 * this class stays easy to read.
 *
 * <p><b>How the pieces fit together:</b>
 * <ul>
 *   <li>{@code @RestController} — responses are serialized straight to JSON.</li>
 *   <li>{@code @RequestMapping("/api/v1/accounts")} — the shared path prefix
 *       for every method here (requirement #1: sensible endpoint paths).</li>
 *   <li>The services are injected via the constructor (requirement #2:
 *       dependency injection).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account management operations")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    /**
     * {@code POST /api/v1/accounts} — create a new account.
     *
     * <p>{@code @Valid} triggers the validation rules on
     * {@link CreateAccountRequest} before this method runs; invalid input never
     * reaches the service. On success we return {@code 201 Created}.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request.getOwnerName(), request.getInitialBalance());
    }

    @GetMapping
    @Operation(summary = "List all accounts")
    public List<AccountResponse> getAccounts() {
        return accountService.getAccounts();
    }

    /**
     * {@code GET /api/v1/accounts/{accountId}} — fetch a single account.
     *
     * <p>Spring converts the {@code {accountId}} path segment into a
     * {@link UUID}. A missing account results in a 404 via the global handler.
     */
    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public AccountResponse getAccount(@PathVariable UUID accountId) {
        return accountService.getAccount(accountId);
    }

    /**
     * {@code GET /api/v1/accounts/{accountId}/transactions} — paginated history.
     *
     * <p>{@code page} and {@code size} are optional query parameters; when
     * omitted the service falls back to sensible defaults.
     */
    @GetMapping("/{accountId}/transactions")
    @Operation(summary = "Get paginated transaction history for an account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction history returned"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public PageResponse<TransactionResponse> getTransactionHistory(
            @PathVariable UUID accountId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return transactionService.getTransactionHistory(accountId, page, size);
    }
}
