package com.fintech.wallet.controller;

import com.fintech.wallet.BaseIntegrationTest;
import com.fintech.wallet.entity.Account;
import com.fintech.wallet.exception.InsufficientFundsException;
import com.fintech.wallet.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for concurrent transfer correctness.
 * 
 * **Property 13: Concurrent Transfer Correctness**
 * **Validates: Requirements 1.3, 11.1, 11.2**
 * 
 * This test verifies that pessimistic locking correctly handles concurrent transfers
 * to prevent race conditions. When two concurrent transfers of 600 each are attempted
 * from an account with balance 1000, exactly one should succeed and one should fail.
 * 
 * Validates: Requirements 1.1, 1.2, 1.3, 3.1, 3.2, 11.1, 11.2
 */
class ConcurrentTransferIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private TransferService transferService;
    
    @Test
    void testConcurrentTransfers_PessimisticLockingPreventsRaceConditions() throws InterruptedException {
        // Step 1: Create account with balance 1000
        LocalDateTime now = LocalDateTime.now();
        Account sender = new Account(null, 100L, new BigDecimal("1000.00"), null, now, now);
        Account receiver1 = new Account(null, 101L, new BigDecimal("0.00"), null, now, now);
        Account receiver2 = new Account(null, 102L, new BigDecimal("0.00"), null, now, now);
        
        sender = accountRepository.save(sender);
        receiver1 = accountRepository.save(receiver1);
        receiver2 = accountRepository.save(receiver2);
        
        Long senderId = sender.getId();
        Long receiver1Id = receiver1.getId();
        Long receiver2Id = receiver2.getId();
        
        // Step 2: Execute two concurrent transfers of 600 each using ExecutorService
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        Runnable transfer1 = () -> {
            try {
                transferService.transferMoney(senderId, receiver1Id, new BigDecimal("600.00"));
                successCount.incrementAndGet();
            } catch (InsufficientFundsException e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        };
        
        Runnable transfer2 = () -> {
            try {
                transferService.transferMoney(senderId, receiver2Id, new BigDecimal("600.00"));
                successCount.incrementAndGet();
            } catch (InsufficientFundsException e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        };
        
        // Execute both transfers concurrently
        executor.submit(transfer1);
        executor.submit(transfer2);
        
        // Wait for both transfers to complete
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Step 3: Verify exactly one succeeds and one fails with InsufficientFundsException
        assertEquals(1, successCount.get(), "Exactly one transfer should succeed");
        assertEquals(1, failureCount.get(), "Exactly one transfer should fail with InsufficientFundsException");
        
        // Step 4: Verify final balance is 400 (1000 - 600)
        Account finalSender = accountRepository.findById(senderId).orElseThrow();
        assertEquals(new BigDecimal("400.00"), finalSender.getBalance(), 
            "Sender balance should be 400 (1000 - 600)");
        
        // Step 5: Verify pessimistic locking prevents race conditions
        // Verify that one receiver has 600, the other has 0
        Account finalReceiver1 = accountRepository.findById(receiver1Id).orElseThrow();
        Account finalReceiver2 = accountRepository.findById(receiver2Id).orElseThrow();
        
        BigDecimal totalReceived = finalReceiver1.getBalance().add(finalReceiver2.getBalance());
        assertEquals(new BigDecimal("600.00"), totalReceived, 
            "Total received should be 600 (one transfer succeeded)");
        
        // Verify balance conservation: total system balance unchanged
        BigDecimal totalSystemBalance = finalSender.getBalance()
            .add(finalReceiver1.getBalance())
            .add(finalReceiver2.getBalance());
        assertEquals(new BigDecimal("1000.00"), totalSystemBalance, 
            "Total system balance should remain 1000 (balance conservation)");
        
        // Verify that exactly one receiver got the money (not both)
        boolean receiver1GotMoney = finalReceiver1.getBalance().compareTo(BigDecimal.ZERO) > 0;
        boolean receiver2GotMoney = finalReceiver2.getBalance().compareTo(BigDecimal.ZERO) > 0;
        assertTrue(receiver1GotMoney ^ receiver2GotMoney, 
            "Exactly one receiver should have received money (XOR condition)");
    }
}
