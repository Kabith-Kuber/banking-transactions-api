package com.brainridge.banking.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BankingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullFlow_createTransferAndFetchHistory() throws Exception {
        MvcResult firstAccount = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"Alice","initialBalance":1000.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult secondAccount = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"Bob","initialBalance":500.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String fromAccountId = objectMapper.readTree(firstAccount.getResponse().getContentAsString()).get("id").asText();
        String toAccountId = objectMapper.readTree(secondAccount.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromAccountId":"%s",
                                  "toAccountId":"%s",
                                  "amount":150.00,
                                  "description":"Rent"
                                }
                                """.formatted(fromAccountId, toAccountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(150.00));

        mockMvc.perform(get("/api/v1/accounts/{accountId}", fromAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(850.00));

        mockMvc.perform(get("/api/v1/accounts/{accountId}", toAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(650.00));

        MvcResult history = mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", fromAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn();

        JsonNode firstTransaction = objectMapper.readTree(history.getResponse().getContentAsString())
                .get("content")
                .get(0);
        assertEquals("Rent", firstTransaction.get("description").asText());
    }

    @Test
    void transfer_returnsNotFoundForMissingAccount() throws Exception {
        String missingId = "00000000-0000-0000-0000-000000000001";
        String existingId = objectMapper.readTree(mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"Alice","initialBalance":100.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromAccountId":"%s",
                                  "toAccountId":"%s",
                                  "amount":10.00
                                }
                                """.formatted(existingId, missingId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void transfer_returnsUnprocessableEntityForInsufficientFunds() throws Exception {
        String fromAccountId = objectMapper.readTree(mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"Alice","initialBalance":50.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asText();

        String toAccountId = objectMapper.readTree(mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"Bob","initialBalance":10.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromAccountId":"%s",
                                  "toAccountId":"%s",
                                  "amount":100.00
                                }
                                """.formatted(fromAccountId, toAccountId)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createAccount_returnsBadRequestForInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"","initialBalance":-1.00}
                                """))
                .andExpect(status().isBadRequest());
    }
}
