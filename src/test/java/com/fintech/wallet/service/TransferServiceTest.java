package com.fintech.wallet.service;

import com.fintech.wallet.dto.TransferResponse;
import com.fintech.wallet.entity.Account;
import com.fintech.wallet.entity.Transaction;
import com.fintech.wallet.entity.TransactionStatus;
import com.fintech.wallet.exception.AccountNotFoundException;
import com.fintech.wallet.exception.InsufficientFundsException;
import com.fintech.wallet.repository.AccountRepository;
import com.fintech.wallet.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransferService.
 * 
 * Validates: Requirements 2.1, 2.2, 3.1, 3.2, 4.1, 4.2, 5.1, 5.2
 */
@SpringBootTest
@Transactional
class TransferServiceTest {
    
    @Autowired
    private TransferService transferService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    private Account senderAccount;
    private Account receiverAccount;
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        
        // Create test accounts
        senderAccount = new Account(
            null, 
            100L, 
            new BigDecimal("1000.00"), 
            null, 
            LocalDateTime.now(), 
            LocalDateTime.now()
        );
        
        receiverAccount = new Account(
            null, 
            200L, 
            new BigDecimal("500.00"), 
            null, 
            LocalDateTime.now(), 
            LocalDateTime.now()
        );
        
        senderAccount = accountRepository.save(senderAccount);
        receiverAccount = accountRepository.save(receiverAccount);
    }
    
    /**
     * Test successful transfer between valid accounts.
     * 
     * Validates: Requirements 2.1, 2.3, 6.1, 6.2, 6.3
     */
    @Test
    void testSuccessfulTransfer() {
        // Arrange
        BigDecimal transferAmount = new BigDecimal("100.00");
        BigDecimal expectedSenderBalance = new BigDecimal("900.00");
        BigDecimal expectedReceiverBalance = new BigDecimal("600.00");
        
        // Act
        TransferResponse response = transferService.transferMoney(
            senderAccount.getId(),
            receiverAccount.getId(),
            transferAmount
        );
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getTransactionId());
        assertEquals(TransactionStatus.COMPLETED, response.getStatus());
        assertEquals("Transfer successful", response.getMessage());
        assertNotNull(response.getTimestamp());
        
        // Verify account balances
        Account updatedSender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        Account updatedReceiver = accountRepository.findById(receiverAccount.getId()).orElseThrow();
        
        assertEquals(expectedSenderBalance, updatedSender.getBalance());
        assertEquals(expectedReceiverBalance, updatedReceiver.getBalance());
        
        // Verify transaction record created
        Transaction transaction = transactionRepository.findById(response.getTransactionId()).orElseThrow();
        assertEquals(senderAccount.getId(), transaction.getSenderId());
        assertEquals(receiverAccount.getId(), transaction.getReceiverId());
        assertEquals(transferAmount, transaction.getAmount());
        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
        assertNotNull(transaction.getTimestamp());
    }
    
    /**
     * Test transfer fails with InsufficientFundsException when balance too low.
     * 
     * Validates: Requirements 3.1, 3.2, 3.3
     */
    @Test
    void testInsufficientFunds() {
        // Arrange
        BigDecimal transferAmount = new BigDecimal("2000.00"); // More than sender has
        BigDecimal originalSenderBalance = senderAccount.getBalance();
        BigDecimal originalReceiverBalance = receiverAccount.getBalance();
        
        // Act & Assert
        InsufficientFundsException exception = assertThrows(
            InsufficientFundsException.class,
            () -> transferService.transferMoney(
                senderAccount.getId(),
                receiverAccount.getId(),
                transferAmount
            )
        );
        
        // Verify exception message
        assertTrue(exception.getMessage().contains("insufficient"));
        assertTrue(exception.getMessage().contains(senderAccount.getId().toString()));
        
        // Verify balances unchanged (rollback)
        Account updatedSender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        Account updatedReceiver = accountRepository.findById(receiverAccount.getId()).orElseThrow();
        
        assertEquals(originalSenderBalance, updatedSender.getBalance());
        assertEquals(originalReceiverBalance, updatedReceiver.getBalance());
        
        // Verify no transaction record created
        assertEquals(0, transactionRepository.count());
    }
    
    /**
     * Test transfer fails with AccountNotFoundException when sender doesn't exist.
     * 
     * Validates: Requirements 4.1, 4.3
     */
    @Test
    void testSenderAccountNotFound() {
        // Arrange
        Long nonExistentSenderId = 99999L;
        BigDecimal transferAmount = new BigDecimal("100.00");
        
        // Act & Assert
        AccountNotFoundException exception = assertThrows(
            AccountNotFoundException.class,
            () -> transferService.transferMoney(
                nonExistentSenderId,
                receiverAccount.getId(),
                transferAmount
            )
        );
        
        // Verify exception message
        assertTrue(exception.getMessage().contains("Sender"));
        assertTrue(exception.getMessage().contains(nonExistentSenderId.toString()));
        
        // Verify no transaction record created
        assertEquals(0, transactionRepository.count());
    }
    
    /**
     * Test transfer fails with AccountNotFoundException when receiver doesn't exist.
     * 
     * Validates: Requirements 4.2, 4.4
     */
    @Test
    void testReceiverAccountNotFound() {
        // Arrange
        Long nonExistentReceiverId = 99999L;
        BigDecimal transferAmount = new BigDecimal("100.00");
        BigDecimal originalSenderBalance = senderAccount.getBalance();
        
        // Act & Assert
        AccountNotFoundException exception = assertThrows(
            AccountNotFoundException.class,
            () -> transferService.transferMoney(
                senderAccount.getId(),
                nonExistentReceiverId,
                transferAmount
            )
        );
        
        // Verify exception message
        assertTrue(exception.getMessage().contains("Receiver"));
        assertTrue(exception.getMessage().contains(nonExistentReceiverId.toString()));
        
        // Verify sender balance unchanged (rollback)
        Account updatedSender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        assertEquals(originalSenderBalance, updatedSender.getBalance());
        
        // Verify no transaction record created
        assertEquals(0, transactionRepository.count());
    }
    
    /**
     * Test transfer fails with IllegalArgumentException for zero amount.
     * 
     * Validates: Requirements 5.1, 5.2
     */
    @Test
    void testZeroAmount() {
        // Arrange
        BigDecimal zeroAmount = BigDecimal.ZERO;
        BigDecimal originalSenderBalance = senderAccount.getBalance();
        BigDecimal originalReceiverBalance = receiverAccount.getBalance();
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transferService.transferMoney(
                senderAccount.getId(),
                receiverAccount.getId(),
                zeroAmount
            )
        );
        
        // Verify exception message
        assertTrue(exception.getMessage().contains("positive"));
        
        // Verify balances unchanged
        Account updatedSender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        Account updatedReceiver = accountRepository.findById(receiverAccount.getId()).orElseThrow();
        
        assertEquals(originalSenderBalance, updatedSender.getBalance());
        assertEquals(originalReceiverBalance, updatedReceiver.getBalance());
        
        // Verify no transaction record created
        assertEquals(0, transactionRepository.count());
    }
    
    /**
     * Test transfer fails with IllegalArgumentException for negative amount.
     * 
     * Validates: Requirements 5.1, 5.2
     */
    @Test
    void testNegativeAmount() {
        // Arrange
        BigDecimal negativeAmount = new BigDecimal("-50.00");
        BigDecimal originalSenderBalance = senderAccount.getBalance();
        BigDecimal originalReceiverBalance = receiverAccount.getBalance();
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transferService.transferMoney(
                senderAccount.getId(),
                receiverAccount.getId(),
                negativeAmount
            )
        );
        
        // Verify exception message
        assertTrue(exception.getMessage().contains("positive"));
        
        // Verify balances unchanged
        Account updatedSender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        Account updatedReceiver = accountRepository.findById(receiverAccount.getId()).orElseThrow();
        
        assertEquals(originalSenderBalance, updatedSender.getBalance());
        assertEquals(originalReceiverBalance, updatedReceiver.getBalance());
        
        // Verify no transaction record created
        assertEquals(0, transactionRepository.count());
    }
    
    /**
     * Test transfer fails with IllegalArgumentException for null amount.
     * 
     * Validates: Requirements 5.1, 5.2
     */
    @Test
    void testNullAmount() {
        // Arrange
        BigDecimal originalSenderBalance = senderAccount.getBalance();
        BigDecimal originalReceiverBalance = receiverAccount.getBalance();
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transferService.transferMoney(
                senderAccount.getId(),
                receiverAccount.getId(),
                null
            )
        );
        
        // Verify exception message
        assertTrue(exception.getMessage().contains("positive"));
        
        // Verify balances unchanged
        Account updatedSender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        Account updatedReceiver = accountRepository.findById(receiverAccount.getId()).orElseThrow();
        
        assertEquals(originalSenderBalance, updatedSender.getBalance());
        assertEquals(originalReceiverBalance, updatedReceiver.getBalance());
        
        // Verify no transaction record created
        assertEquals(0, transactionRepository.count());
    }
    
    /**
     * Test getTransactionById returns correct transaction.
     * 
     * Validates: Requirements 6.2, 6.3
     */
    @Test
    void testGetTransactionById() {
        // Arrange: Perform a transfer first
        BigDecimal transferAmount = new BigDecimal("100.00");
        TransferResponse response = transferService.transferMoney(
            senderAccount.getId(),
            receiverAccount.getId(),
            transferAmount
        );
        
        // Act
        Transaction transaction = transferService.getTransactionById(response.getTransactionId());
        
        // Assert
        assertNotNull(transaction);
        assertEquals(response.getTransactionId(), transaction.getId());
        assertEquals(senderAccount.getId(), transaction.getSenderId());
        assertEquals(receiverAccount.getId(), transaction.getReceiverId());
        assertEquals(transferAmount, transaction.getAmount());
        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
    }
    
    /**
     * Test getTransactionById throws exception for non-existent transaction.
     * 
     * Validates: Requirements 6.2, 6.3
     */
    @Test
    void testGetTransactionByIdNotFound() {
        // Arrange
        Long nonExistentTransactionId = 99999L;
        
        // Act & Assert
        AccountNotFoundException exception = assertThrows(
            AccountNotFoundException.class,
            () -> transferService.getTransactionById(nonExistentTransactionId)
        );
        
        // Verify exception message
        assertTrue(exception.getMessage().contains("Transaction"));
        assertTrue(exception.getMessage().contains(nonExistentTransactionId.toString()));
    }
}
