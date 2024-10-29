package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.dto.ResponseWrapper;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.EmptyRequestBodyException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.TransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;

  private final TransferService transferService;

  @Autowired
  public AccountsController(AccountsService accountsService, TransferService transferService) {
    this.accountsService = accountsService;
    this.transferService = transferService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody(required = false) @Valid Account account) {
    if (account == null) {
      throw new EmptyRequestBodyException("Request body is missing.");
    }

    log.info("Creating account {}", account);

    try {
    this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {

      ResponseWrapper<Object> duplicateAccountResponse = new ResponseWrapper<>(
              null,
              daie.getMessage(),
              HttpStatus.BAD_REQUEST.value()
      );

      return new ResponseEntity<>(duplicateAccountResponse, HttpStatus.BAD_REQUEST);
    }

    ResponseWrapper<Object> successResponse = new ResponseWrapper<>(
            null,
            "Account created successfully",
            HttpStatus.CREATED.value()
    );


    return new ResponseEntity<>(successResponse,HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public ResponseEntity<ResponseWrapper<Account>> getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    ResponseWrapper<Account> response = new ResponseWrapper<>(
            this.accountsService.getAccount(accountId),
            "Account retrieved successfully",
            HttpStatus.OK.value()
    );

    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/getAllAccounts")
  public ResponseEntity<ResponseWrapper<List<Account>>> getAllAccounts() {
    log.info("Retrieving all accounts");
    List<Account> accounts = accountsService.getAllAccounts().stream()
            .map(account -> new Account(account.getAccountId(), account.getBalance()))
            .collect(Collectors.toList());

    ResponseWrapper<List<Account>> response = new ResponseWrapper<>(
            accounts,
            "Accounts retrieved successfully",
            HttpStatus.OK.value()
    );

    return ResponseEntity.ok(response);
  }

  @PostMapping(path = "/transfer")
  public ResponseEntity<ResponseWrapper<Object>> transfer(@Valid @RequestBody TransferRequest transferRequest) {
    if (transferRequest.getAmount() == null || transferRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      ResponseWrapper<Object> response = new ResponseWrapper<>(
              null,
              "Transfer amount must be positive",
              HttpStatus.BAD_REQUEST.value()
      );
      return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
    }

      transferService.transfer(
              transferRequest.getAccountFromId(),
              transferRequest.getAccountToId(),
              transferRequest.getAmount()
      );

      ResponseWrapper<Object> response = new ResponseWrapper<>(
              null,
              "Transfer successful",
              HttpStatus.OK.value()
      );
      return new ResponseEntity<>(response,HttpStatus.OK);

  }
}
