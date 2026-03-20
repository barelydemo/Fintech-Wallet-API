package com.fintech.wallet.service;

import com.fintech.wallet.entity.Account;
import com.fintech.wallet.exception.AccountNotFoundException;
import com.fintech.wallet.exception.InsufficientFundsException;
import com.fintech.wallet.repository.AccountRepository;
import com.fintech.wallet.repository.TransactionRepository;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for rollback on error scenarios.
 * 
 * **Property 9: Rollback on Error**
 * **Validates: Requirements 2.2, 3.3**
 * 
 * This test generates various error scenarios and verifies that the system state
 * remains unchanged after exceptions occur during transfers.
 */
@JqwikSpringSupport
@SpringBootTest
class RollbackOnErrorPropertyTest {
    
    @Autowired
    private TransferService transferService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @AfterEach
    void cleanup() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }
    
    /**
     * Property 9: Rollback on Error - Insufficient Funds Scenario
     * 
     * Verifies that when a transfer fails due to insufficient funds,
     * all account balances remain exactly as they were before the transfer attempt.
     */
    @Property(tries = 20)
    void rollbackOnInsufficientFunds(
        @ForAll("smallBalances") BigDecimal senderBalance,
        @ForAll("largeAmounts") BigDecimal transferAmount,
        @ForAll("receiverBalances") BigDecimal receiverBalance
    ) {
        // Arrange: Create accounts where sender has insufficient funds
        Account sender = new Account(null, 1L, senderBalance, null, LocalDateTime.now(), LocalDateTime.now());
        Account receiver = new Account(null, 2L, receiverBalance, null, LocalDateTime.now(), LocalDateTime.now());
        
        sender = accountRepository.save(sender);
        receiver = accountRepository.save(receiver);
        
        final Long senderId = sender.getId();
        final Long receiverId = receiver.getId();
        
        BigDecimal senderBalanceBefore = sender.getBalance();
        BigDecimal receiverBalanceBefore = receiver.getBalance();
        BigDecimal totalBalanceBefore = senderBalanceBefore.add(receiverBalanceBefore);
        
        // Act: Attempt transfer that will fail
        assertThrows(
            InsufficientFundsException.class,
            () -> transferService.transferMoney(senderId, receiverId, transferAmount),
            "Transfer with insufficient funds must throw InsufficientFundsException"
        );
        
        // Assert: Verify system state unchanged after exception
        Account senderAfter = accountRepository.findById(senderId).orElseThrow();
        Account receiverAfter = accountRepository.findById(receiverId).orElseThrow();
        
        assertEquals(
            senderBalanceBefore.stripTrailingZeros(),
            senderAfter.getBalance().stripTrailingZeros(),
            "Sender balance must be unchanged after failed transfer"
        );
        assertEquals(
            receiverBalanceBefore.stripTrailingZeros(),
            receiverAfter.getBalance().stripTrailingZeros(),
            "Receiver balance must be unchanged after failed transfer"
        );
        
        // Verify total system balance unchanged
        BigDecimal totalBalanceAfter = senderAfter.getBalance().add(receiverAfter.getBalance());
        assertEquals(
            totalBalanceBefore.stripTrailingZeros(),
            totalBalanceAfter.stripTrailingZeros(),
            "Total system balance must remain unchanged after failed transfer"
        );
        
        // Verify no transaction record created
        long transactionCount = transactionRepository.count();
        assertEquals(0, transactionCount, "No transaction record should exist after failed transfer");
        
        // Cleanup
        accountRepository.deleteById(senderId);
        accountRepository.deleteById(receiverId);
    }
    
    /**
     * Property 9: Rollback on Error - Non-Existent Sender Account Scenario
     * 
     * Verifies that when a transfer fails due to non-existent sender account,
     * the receiver account balance remains unchanged.
     */
    @Property(tries = 20)
    void rollbackOnNonExistentSender(
        @ForAll("receiverBalances") BigDecimal receiverBalance,
        @ForAll("transferAmounts") BigDecimal transferAmount
    ) {
        // Arrange: Create only receiver account (sender doesn't exist)
        Account receiver = new Account(null, 2L, receiverBalance, null, LocalDateTime.now(), LocalDateTime.now());
        receiver = accountRepository.save(receiver);
        
        final Long nonExistentSenderId = 999999L;
        final Long receiverId = receiver.getId();
        
        BigDecimal receiverBalanceBefore = receiver.getBalance();
        
        // Act: Attempt transfer with non-existent sender
        assertThrows(
            AccountNotFoundException.class,
            () -> transferService.transferMoney(nonExistentSenderId, receiverId, transferAmount),
            "Transfer with non-existent sender must throw AccountNotFoundException"
        );
        
        // Assert: Verify receiver balance unchanged
        Account receiverAfter = accountRepository.findById(receiverId).orElseThrow();
        assertEquals(
            receiverBalanceBefore.stripTrailingZeros(),
            receiverAfter.getBalance().stripTrailingZeros(),
            "Receiver balance must be unchanged when sender doesn't exist"
        );
        
        // Verify no transaction record created
        long transactionCount = transactionRepository.count();
        assertEquals(0, transactionCount, "No transaction record should exist after failed transfer");
        
        // Cleanup
        accountRepository.deleteById(receiverId);
    }
    
    /**
     * Property 9: Rollback on Error - Non-Existent Receiver Account Scenario
     * 
     * Verifies that when a transfer fails due to non-existent receiver account,
     * the sender account balance remains unchanged.
     */
    @Property(tries = 20)
    void rollbackOnNonExistentReceiver(
        @ForAll("senderBalances") BigDecimal senderBalance,
        @ForAll("transferAmounts") BigDecimal transferAmount
    ) {
        // Arrange: Create only sender account (receiver doesn't exist)
        Account sender = new Account(null, 1L, senderBalance, null, LocalDateTime.now(), LocalDateTime.now());
        sender = accountRepository.save(sender);
        
        final Long senderId = sender.getId();
        final Long nonExistentReceiverId = 999999L;
        
        BigDecimal senderBalanceBefore = sender.getBalance();
        
        // Act: Attempt transfer with non-existent receiver
        assertThrows(
            AccountNotFoundException.class,
            () -> transferService.transferMoney(senderId, nonExistentReceiverId, transferAmount),
            "Transfer with non-existent receiver must throw AccountNotFoundException"
        );
        
        // Assert: Verify sender balance unchanged
        Account senderAfter = accountRepository.findById(senderId).orElseThrow();
        assertEquals(
            senderBalanceBefore.stripTrailingZeros(),
            senderAfter.getBalance().stripTrailingZeros(),
            "Sender balance must be unchanged when receiver doesn't exist"
        );
        
        // Verify no transaction record created
        long transactionCount = transactionRepository.count();
        assertEquals(0, transactionCount, "No transaction record should exist after failed transfer");
        
        // Cleanup
        accountRepository.deleteById(senderId);
    }
    
    /**
     * Property 9: Rollback on Error - Invalid Amount Scenario
     * 
     * Verifies that when a transfer fails due to invalid amount (zero or negative),
     * all account balances remain unchanged.
     */
    @Property(tries = 20)
    void rollbackOnInvalidAmount(
        @ForAll("senderBalances") BigDecimal senderBalance,
        @ForAll("receiverBalances") BigDecimal receiverBalance,
        @ForAll("invalidAmounts") BigDecimal invalidAmount
    ) {
        // Arrange: Create accounts with valid balances
        Account sender = new Account(null, 1L, senderBalance, null, LocalDateTime.now(), LocalDateTime.now());
        Account receiver = new Account(null, 2L, receiverBalance, null, LocalDateTime.now(), LocalDateTime.now());
        
        sender = accountRepository.save(sender);
        receiver = accountRepository.save(receiver);
        
        final Long senderId = sender.getId();
        final Long receiverId = receiver.getId();
        
        BigDecimal senderBalanceBefore = sender.getBalance();
        BigDecimal receiverBalanceBefore = receiver.getBalance();
        
        // Act: Attempt transfer with invalid amount
        assertThrows(
            IllegalArgumentException.class,
            () -> transferService.transferMoney(senderId, receiverId, invalidAmount),
            "Transfer with invalid amount must throw IllegalArgumentException"
        );
        
        // Assert: Verify balances unchanged
        Account senderAfter = accountRepository.findById(senderId).orElseThrow();
        Account receiverAfter = accountRepository.findById(receiverId).orElseThrow();
        
        assertEquals(
            senderBalanceBefore.stripTrailingZeros(),
            senderAfter.getBalance().stripTrailingZeros(),
            "Sender balance must be unchanged after invalid amount error"
        );
        assertEquals(
            receiverBalanceBefore.stripTrailingZeros(),
            receiverAfter.getBalance().stripTrailingZeros(),
            "Receiver balance must be unchanged after invalid amount error"
        );
        
        // Verify no transaction record created
        long transactionCount = transactionRepository.count();
        assertEquals(0, transactionCount, "No transaction record should exist after failed transfer");
        
        // Cleanup
        accountRepository.deleteById(senderId);
        accountRepository.deleteById(receiverId);
    }
    
    // Custom arbitraries for BigDecimal with specific ranges
    
    @Provide
    Arbitrary<BigDecimal> senderBalances() {
        return Arbitraries.bigDecimals()
            .between(new BigDecimal("100.00"), new BigDecimal("10000.00"))
            .ofScale(2);
    }
    
    @Provide
    Arbitrary<BigDecimal> receiverBalances() {
        return Arbitraries.bigDecimals()
            .between(new BigDecimal("0.00"), new BigDecimal("10000.00"))
            .ofScale(2);
    }
    
    @Provide
    Arbitrary<BigDecimal> transferAmounts() {
        return Arbitraries.bigDecimals()
            .between(new BigDecimal("0.01"), new BigDecimal("100.00"))
            .ofScale(2);
    }
    
    @Provide
    Arbitrary<BigDecimal> smallBalances() {
        return Arbitraries.bigDecimals()
            .between(new BigDecimal("0.00"), new BigDecimal("100.00"))
            .ofScale(2);
    }
    
    @Provide
    Arbitrary<BigDecimal> largeAmounts() {
        return Arbitraries.bigDecimals()
            .between(new BigDecimal("100.01"), new BigDecimal("500.00"))
            .ofScale(2);
    }
    
    @Provide
    Arbitrary<BigDecimal> invalidAmounts() {
        return Arbitraries.bigDecimals()
            .between(new BigDecimal("-100.00"), new BigDecimal("0.00"))
            .ofScale(2);
    }
}
