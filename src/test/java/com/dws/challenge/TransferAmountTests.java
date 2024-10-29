package com.dws.challenge;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepositoryInMemory;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.*;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
public class TransferAmountTests {


    private MockMvc mockMvc;

    @MockBean
    private AccountsRepositoryInMemory accountRepository; // Mock repository

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void prepareMockMvc() throws Exception {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        Account account1 = new Account("1", new BigDecimal(1000));
        Account account2 = new Account("2", new BigDecimal(500));

        // Mock repository behavior for fetching accounts
        when(accountRepository.getAccount("1")).thenReturn(account1);
        when(accountRepository.getAccount("2")).thenReturn(account2);

        // Mock repository behavior for saving updated accounts
        doAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            if (account.getAccountId().equals("1")) {
                account1.setBalance(account.getBalance());
            } else if (account.getAccountId().equals("2")) {
                account2.setBalance(account.getBalance());
            }
            return null;
        }).when(accountRepository).updateAccount(any(Account.class));
    }


    @Test
    void transferMoney_SuccessfulTransfer() throws Exception {
        mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\": \"1\", \"accountToId\": \"2\", \"amount\": 100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transfer successful"))
                .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    void transferMoney_InsufficientBalance() throws Exception {
        mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\": \"1\", \"accountToId\": \"2\", \"amount\": 2000}")) // Assume balance < 1000
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient balance in 1 account"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void transferMoney_NegativeAmount() throws Exception {
        mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\": \"1\", \"accountToId\": \"2\", \"amount\": -50}")) //Transfer negative amount
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transfer amount must be positive"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void transferMoney_ZeroAmount() throws Exception {
        mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\": \"1\", \"accountToId\": \"2\", \"amount\": 0}")) //Transfer 0 amount
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transfer amount must be positive"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }
    @Test
    void transferMoney_FromAccountMissing() throws Exception {
        mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\": \"99\", \"accountToId\": \"2\", \"amount\": 200}")) //From account missing
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account number 99 not found"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void transferMoney_ToAccountMissing() throws Exception {
        mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\": \"1\", \"accountToId\": \"898\", \"amount\": 200}")) //To account missing
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account number 898 not found"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void transferMoney_SameAccountTransfer() throws Exception {
        mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\": \"1\", \"accountToId\": \"1\", \"amount\": 50}")) // Same account numbers
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transfer must be between two different accounts"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void transferMoney_MissingFields() throws Exception {
        mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\": \"1\", \"amount\": 50.0}")) // Missing accountToId
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void transferMoney_ConcurrentTransfers() throws Exception {
        // Assume these initial balances for testing
        BigDecimal initialBalanceFrom = new BigDecimal(1000);
        BigDecimal initialBalanceTo = new BigDecimal(500);
        BigDecimal transferAmount = new BigDecimal(10);;
        int numTransfers = 10;

        // Initialize accounts in repository (this may vary based on your setup)
        accountRepository.createAccount(new Account("1", initialBalanceFrom));
        accountRepository.createAccount(new Account("2", initialBalanceTo));

        CountDownLatch latch = new CountDownLatch(numTransfers);
        ExecutorService executor = Executors.newFixedThreadPool(numTransfers);

        for (int i = 0; i < numTransfers; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(post("/v1/accounts/transfer")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"accountFromId\": \"1\", \"accountToId\": \"2\", \"amount\": " + transferAmount + "}"))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();  // Wait until all transfers are completed
        executor.shutdown();

        // Inspect final balances
        Account accountFrom = accountRepository.getAccount("1");
        Account accountTo = accountRepository.getAccount("2");

        // Calculate expected balances
        BigDecimal expectedFinalBalanceFrom = initialBalanceFrom.subtract(transferAmount.multiply(new BigDecimal(numTransfers)));
        BigDecimal expectedFinalBalanceTo = initialBalanceTo.add(transferAmount.multiply(new BigDecimal(numTransfers)));

        // Verify the final balances
        Assertions.assertEquals(expectedFinalBalanceFrom, accountFrom.getBalance(), "Final balance of accountFrom is incorrect");
        Assertions.assertEquals(expectedFinalBalanceTo, accountTo.getBalance(), "Final balance of accountTo is incorrect");
    }









}
