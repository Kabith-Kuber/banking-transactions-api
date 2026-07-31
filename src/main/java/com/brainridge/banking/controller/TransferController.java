package com.brainridge.banking.controller;

import com.brainridge.banking.dto.request.TransferRequest;
import com.brainridge.banking.dto.response.TransferResponse;
import com.brainridge.banking.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point for {@code /api/v1/transfers} (the "Controller" layer).
 *
 * <p>Transfers get their own endpoint because moving money is a distinct action
 * rather than a property of one account. Like the account controller, this
 * class only unpacks the request and delegates to {@link TransferService}.
 */
@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers", description = "Fund transfer operations")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * {@code POST /api/v1/transfers} — move money between two accounts.
     *
     * <p>{@code @Valid} enforces the basic input rules on {@link TransferRequest}
     * first. The service then applies the money rules and may raise a
     * not-found (404) or insufficient-funds (422) error, which the global
     * handler formats. On success we return {@code 201 Created}.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer funds between accounts")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer completed"),
            @ApiResponse(responseCode = "400", description = "Invalid transfer request"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "422", description = "Insufficient funds")
    })
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return transferService.transfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getDescription()
        );
    }
}
