package com.fintech.wallet.controller;

import com.fintech.wallet.BaseIntegrationTest;
import com.fintech.wallet.dto.TransferRequest;
import com.fintech.wallet.dto.TransferResponse;
import com.fintech.wallet.entity.Account;
import com.fintech.wallet.entity.Transaction;
import com.fintech.wallet.entity.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test for money transfer flow.
 * 
 * This test verifies the complete transfer workflow:
 * - Account creation via repository
 * - Transfer execution via REST API
 * - Database state verification
 * - Response validation
 * 
 * Validates: Requirements 2.1, 2.3, 6.1, 6.2, 6.3, 7.3
 */
class TransferEndToEndIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testEndToEndTransfer_Success() {
        // Step 1: Create accounts via repository
        LocalDateTime now = LocalDateTime.now();
        Account sender = new Account(null, 100L, new BigDecimal("1000.00"), null, now, now);
        Account receiver = new Account(null, 101L, new BigDecimal("500.00"), null, now, now);
        
        sender = accountRepository.save(sender);
        receiver = accountRepository.save(receiver);
        
        Long senderId = sender.getId();
        Long receiverId = receiver.getId();
        BigDecimal transferAmount = new BigDecimal("250.00");
        
        // Step 2: Execute transfer via REST API using TestRestTemplate
        TransferRequest request = new TransferRequest(senderId, receiverId, transferAmount);
        
        ResponseEntity<TransferResponse> response = restTemplate.postForEntity(
            "/api/transfers",
            request,
            TransferResponse.class
        );
        
        // Step 3: Verify response contains correct transactionId and status
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        TransferResponse transferResponse = response.getBody();
        assertNotNull(transferResponse);
        assertNotNull(transferResponse.getTransactionId());
        assertEquals(TransactionStatus.COMPLETED, transferResponse.getStatus());
        assertEquals("Transfer successful", transferResponse.getMessage());
        assertNotNull(transferResponse.getTimestamp());
        
        // Step 4: Verify database state directly using repositories
        
        // Verify sender account balance updated correctly
        Account updatedSender = accountRepository.findById(senderId).orElseThrow();
        assertEquals(new BigDecimal("750.00"), updatedSender.getBalance());
        
        // Verify receiver account balance updated correctly
        Account updatedReceiver = accountRepository.findById(receiverId).orElseThrow();
        assertEquals(new BigDecimal("750.00"), updatedReceiver.getBalance());
        
        // Verify transaction record created in database
        Transaction transaction = transactionRepository.findById(transferResponse.getTransactionId()).orElseThrow();
        assertEquals(senderId, transaction.getSenderId());
        assertEquals(receiverId, transaction.getReceiverId());
        assertEquals(transferAmount, transaction.getAmount());
        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
        assertNotNull(transaction.getTimestamp());
        
        // Verify balance conservation (total system balance unchanged)
        BigDecimal totalBalance = updatedSender.getBalance().add(updatedReceiver.getBalance());
        assertEquals(new BigDecimal("1500.00"), totalBalance);
    }
}
