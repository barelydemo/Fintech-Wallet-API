package com.fintech.wallet.service;

import com.fintech.wallet.dto.TransferResponse;
import com.fintech.wallet.entity.Account;
import com.fintech.wallet.entity.TransactionStatus;
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
 * Property-based tests for TransferService.
 * 
 * These tests validate universal correctness properties across many random scenarios.
 */
@JqwikSpringSupport
@SpringBootTest
class TransferServicePropertyTest {
    
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
     * Property 1: Balance Conservation (Money Never Created or Destroyed)
     * 
     * **Validates: Requirements 2.1, 2.3, 2.4**
     * 
     * This property verifies that the total system balance remains constant
     * before and after any transfer. Money is never created or destroyed.
     */
    @Property(tries = 10)
    void balanceConservation(
        @ForAll("senderBalances") BigDecimal senderInitialBalance,
        @ForAll("receiverBalances") BigDecimal receiverInitialBalance,
        @ForAll("transferAmounts") BigDecimal transferAmount
    ) {
        // Arrange: Create accounts with random balances
        Account sender = new Account(null, 1L, senderInitialBalance, null, LocalDateTime.now(), LocalDateTime.now());
        Account receiver = new Account(null, 2L, receiverInitialBalance, null, LocalDateTime.now(), LocalDateTime.now());
        
        sender = accountRepository.save(sender);
        receiver = accountRepository.save(receiver);
        
        BigDecimal totalBalanceBefore = senderInitialBalance.add(receiverInitialBalance);
        
        // Act: Perform transfer if sender has sufficient funds
        if (senderInitialBalance.compareTo(transferAmount) >= 0) {
            TransferResponse response = transferService.transferMoney(
                sender.getId(), 
                receiver.getId(), 
                transferAmount
            );
            
            // Assert: Verify transfer succeeded
            assertNotNull(response);
            assertEquals(TransactionStatus.COMPLETED, response.getStatus());
            
            // Reload accounts from database
            Account senderAfter = accountRepository.findById(sender.getId()).orElseThrow();
            Account receiverAfter = accountRepository.findById(receiver.getId()).orElseThrow();
            
            // Verify balance conservation
            BigDecimal totalBalanceAfter = senderAfter.getBalance().add(receiverAfter.getBalance());
            assertEquals(
                totalBalanceBefore.stripTrailingZeros(), 
                totalBalanceAfter.stripTrailingZeros(),
                "Total system balance must remain constant"
            );
            
            // Verify individual account changes
            assertEquals(
                senderInitialBalance.subtract(transferAmount).stripTrailingZeros(),
                senderAfter.getBalance().stripTrailingZeros(),
                "Sender balance should decrease by transfer amount"
            );
            assertEquals(
                receiverInitialBalance.add(transferAmount).stripTrailingZeros(),
                receiverAfter.getBalance().stripTrailingZeros(),
                "Receiver balance should increase by transfer amount"
            );
        }
        
        // Cleanup
        accountRepository.deleteById(sender.getId());
        accountRepository.deleteById(receiver.getId());
    }
    
    /**
     * Property 2: No Negative Balances
     * 
     * **Validates: Requirements 3.4, 9.2**
     * 
     * This property verifies that no account ever goes negative,
     * even after a sequence of random transfers.
     */
    @Property(tries = 10)
    void noNegativeBalances(
        @ForAll("initialBalances") BigDecimal initialBalance,
        @ForAll("transferAmounts") BigDecimal transferAmount
    ) {
        // Arrange: Create accounts
        Account sender = new Account(null, 1L, initialBalance, null, LocalDateTime.now(), LocalDateTime.now());
        Account receiver = new Account(null, 2L, BigDecimal.ZERO, null, LocalDateTime.now(), LocalDateTime.now());
        
        sender = accountRepository.save(sender);
        receiver = accountRepository.save(receiver);
        
        final Long senderId = sender.getId();
        final Long receiverId = receiver.getId();
        
        // Act: Attempt transfer
        try {
            if (initialBalance.compareTo(transferAmount) >= 0) {
                // Transfer should succeed
                transferService.transferMoney(senderId, receiverId, transferAmount);
                
                // Assert: Verify no negative balances
                Account senderAfter = accountRepository.findById(senderId).orElseThrow();
                Account receiverAfter = accountRepository.findById(receiverId).orElseThrow();
                
                assertTrue(
                    senderAfter.getBalance().compareTo(BigDecimal.ZERO) >= 0,
                    "Sender balance must never be negative"
                );
                assertTrue(
                    receiverAfter.getBalance().compareTo(BigDecimal.ZERO) >= 0,
                    "Receiver balance must never be negative"
                );
            } else {
                // Transfer should fail with InsufficientFundsException
                assertThrows(
                    InsufficientFundsException.class,
                    () -> transferService.transferMoney(senderId, receiverId, transferAmount),
                    "Transfer with insufficient funds should throw InsufficientFundsException"
                );
                
                // Assert: Verify balances unchanged
                Account senderAfter = accountRepository.findById(senderId).orElseThrow();
                Account receiverAfter = accountRepository.findById(receiverId).orElseThrow();
                
                assertEquals(initialBalance.stripTrailingZeros(), senderAfter.getBalance().stripTrailingZeros(), "Sender balance should be unchanged after failed transfer");
                assertEquals(BigDecimal.ZERO.stripTrailingZeros(), receiverAfter.getBalance().stripTrailingZeros(), "Receiver balance should be unchanged after failed transfer");
            }
        } finally {
            // Cleanup
            accountRepository.deleteById(senderId);
            accountRepository.deleteById(receiverId);
        }
    }
    
    /**
     * Property 3: Insufficient Funds Rejection
     * 
     * **Validates: Requirements 3.1, 3.2, 3.3**
     * 
     * This property verifies that transfers with insufficient funds are rejected
     * and no balance changes occur.
     */
    @Property(tries = 10)
    void insufficientFundsRejection(
        @ForAll("smallBalances") BigDecimal senderBalance,
        @ForAll("largeAmounts") BigDecimal transferAmount
    ) {
        // Arrange: Create accounts where sender has less than transfer amount
        Account sender = new Account(null, 1L, senderBalance, null, LocalDateTime.now(), LocalDateTime.now());
        Account receiver = new Account(null, 2L, BigDecimal.ZERO, null, LocalDateTime.now(), LocalDateTime.now());
        
        sender = accountRepository.save(sender);
        receiver = accountRepository.save(receiver);
        
        final Long senderId = sender.getId();
        final Long receiverId = receiver.getId();
        
        BigDecimal senderBalanceBefore = sender.getBalance();
        BigDecimal receiverBalanceBefore = receiver.getBalance();
        
        // Act & Assert: Verify exception thrown
        InsufficientFundsException exception = assertThrows(
            InsufficientFundsException.class,
            () -> transferService.transferMoney(senderId, receiverId, transferAmount),
            "Transfer with insufficient funds must throw InsufficientFundsException"
        );
        
        // Verify exception message contains useful information
        assertTrue(
            exception.getMessage().contains("insufficient"),
            "Exception message should mention insufficient funds"
        );
        
        // Verify no balance changes occurred (rollback)
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
    Arbitrary<BigDecimal> initialBalances() {
        return Arbitraries.bigDecimals()
            .between(new BigDecimal("100.00"), new BigDecimal("1000.00"))
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
            .between(new BigDecimal("100.01"), new BigDecimal("200.00"))
            .ofScale(2);
    }
    
    @Provide
    Arbitrary<BigDecimal> amounts() {
        return Arbitraries.bigDecimals()
            .between(new BigDecimal("0.01"), new BigDecimal("10000.00"))
            .ofScale(2);
    }
}
