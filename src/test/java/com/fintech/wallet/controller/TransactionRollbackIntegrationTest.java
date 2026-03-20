package com.fintech.wallet.controller;

import com.fintech.wallet.BaseIntegrationTest;
import com.fintech.wallet.entity.Account;
import com.fintech.wallet.repository.AccountRepository;
import com.fintech.wallet.repository.TransactionRepository;
import com.fintech.wallet.service.TransferService;
import com.fintech.wallet.service.TransferServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for transaction rollback behavior.
 * 
 * This test verifies that when an exception occurs during a transfer,
 * the transaction is rolled back and no partial updates are committed
 * to the database.
 * 
 * Validates: Requirements 2.2, 3.3
 */
class TransactionRollbackIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private TransferService transferService;
    
    /**
     * Test configuration that provides a spy on TransactionRepository
     * to simulate exceptions during transaction record creation.
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public TransactionRepository transactionRepositorySpy(TransactionRepository realRepository) {
            return spy(realRepository);
        }
        
        @Bean
        @Primary
        public TransferService transferServiceWithSpy(
                AccountRepository accountRepository,
                TransactionRepository transactionRepository) {
            return new TransferServiceImpl(accountRepository, transactionRepository);
        }
    }
    
    @Autowired
    private TransactionRepository transactionRepositorySpy;
    
    @Test
    void testTransactionRollback_NoPartialUpdatesOnException() {
        // Step 1: Create accounts with initial balances
        LocalDateTime now = LocalDateTime.now();
        Account sender = new Account(null, 100L, new BigDecimal("1000.00"), null, now, now);
        Account receiver = new Account(null, 101L, new BigDecimal("500.00"), null, now, now);
        
        sender = accountRepository.save(sender);
        receiver = accountRepository.save(receiver);
        
        Long senderId = sender.getId();
        Long receiverId = receiver.getId();
        BigDecimal initialSenderBalance = sender.getBalance();
        BigDecimal initialReceiverBalance = receiver.getBalance();
        BigDecimal transferAmount = new BigDecimal("250.00");
        
        // Step 2: Configure mock to throw exception during transaction record creation
        // This simulates a failure after balance updates but before commit
        doThrow(new RuntimeException("Simulated database error during transaction save"))
            .when(transactionRepositorySpy).save(any());
        
        // Step 3: Attempt transfer and expect exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transferService.transferMoney(senderId, receiverId, transferAmount);
        });
        
        assertEquals("Simulated database error during transaction save", exception.getMessage());
        
        // Step 4: Verify no partial updates committed to database
        // Refresh accounts from database to get current state
        Account senderAfterRollback = accountRepository.findById(senderId).orElseThrow();
        Account receiverAfterRollback = accountRepository.findById(receiverId).orElseThrow();
        
        // Step 5: Verify account balances unchanged after rollback
        assertEquals(initialSenderBalance, senderAfterRollback.getBalance(),
            "Sender balance should be unchanged after rollback");
        assertEquals(initialReceiverBalance, receiverAfterRollback.getBalance(),
            "Receiver balance should be unchanged after rollback");
        
        // Step 6: Verify no transaction record was created
        long transactionCount = transactionRepository.count();
        assertEquals(0, transactionCount, 
            "No transaction record should exist after rollback");
        
        // Step 7: Verify balance conservation (total system balance unchanged)
        BigDecimal totalBalanceAfter = senderAfterRollback.getBalance()
            .add(receiverAfterRollback.getBalance());
        BigDecimal totalBalanceBefore = initialSenderBalance.add(initialReceiverBalance);
        assertEquals(totalBalanceBefore, totalBalanceAfter,
            "Total system balance should remain unchanged");
    }
}
