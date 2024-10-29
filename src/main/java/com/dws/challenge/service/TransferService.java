package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.TransferException;
import com.dws.challenge.repository.AccountsRepositoryInMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class TransferService {


    private final AccountsRepositoryInMemory accountRepository;
    private final NotificationService notificationService;
    private final Lock lock = new ReentrantLock();

    @Autowired
    public TransferService(AccountsRepositoryInMemory accountRepository, NotificationService notificationService) {
        this.accountRepository = accountRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void transfer(String accountFromId, String accountToId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferException("Transfer amount must be positive");
        }
        if(accountFromId.equals(accountToId)){
            throw new TransferException("Transfer must be between two different accounts");
        }

        Account accountFrom = Optional.ofNullable(accountRepository.getAccount(accountFromId))
                .orElseThrow(() -> new TransferException("Account number "+accountFromId+" not found"));
        Account accountTo = Optional.ofNullable(accountRepository.getAccount(accountToId))
                .orElseThrow(() -> new TransferException("Account number "+accountToId+" not found"));

        lock.lock();
        try {
            if (accountFrom.getBalance().compareTo(amount) < 0) {
                throw new TransferException("Insufficient balance in "+accountFrom.getAccountId()+" account");
            }

            accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
            accountTo.setBalance(accountTo.getBalance().add(amount));

            accountRepository.updateAccount(accountFrom);
            accountRepository.updateAccount(accountTo);

            notificationService.notifyAboutTransfer(accountFrom, "Transferred " + amount + " to account " + accountToId);
            notificationService.notifyAboutTransfer(accountTo, "Received " + amount + " from account " + accountFromId);

        } finally {
            lock.unlock();
        }
    }
}
