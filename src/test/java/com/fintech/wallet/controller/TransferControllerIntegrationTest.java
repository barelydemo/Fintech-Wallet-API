package com.fintech.wallet.controller;

import com.fintech.wallet.dto.TransferRequest;
import com.fintech.wallet.dto.TransferResponse;
import com.fintech.wallet.entity.Account;
import com.fintech.wallet.entity.TransactionStatus;
import com.fintech.wallet.repository.AccountRepository;
import com.fintech.wallet.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TransferController.
 * 
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }
    
    @Test
    void testTransfer_Success() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Account sender = new Account(null, 100L, new BigDecimal("1000.00"), null, now, now);
        Account receiver = new Account(null, 101L, new BigDecimal("500.00"), null, now, now);
        
        sender = accountRepository.save(sender);
        receiver = accountRepository.save(receiver);
        
        TransferRequest request = new TransferRequest(
            sender.getId(),
            receiver.getId(),
            new BigDecimal("250.00")
        );
        
        // Act
        ResponseEntity<TransferResponse> response = restTemplate.postForEntity(
            "/api/transfers",
            request,
            TransferResponse.class
        );
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TransactionStatus.COMPLETED, response.getBody().getStatus());
        assertNotNull(response.getBody().getTransactionId());
        
        // Verify balances
        Account updatedSender = accountRepository.findById(sender.getId()).orElseThrow();
        Account updatedReceiver = accountRepository.findById(receiver.getId()).orElseThrow();
        
        assertEquals(new BigDecimal("750.00"), updatedSender.getBalance());
        assertEquals(new BigDecimal("750.00"), updatedReceiver.getBalance());
    }
    
    @Test
    void testGetTransferStatus_Success() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Account sender = new Account(null, 100L, new BigDecimal("1000.00"), null, now, now);
        Account receiver = new Account(null, 101L, new BigDecimal("500.00"), null, now, now);
        
        sender = accountRepository.save(sender);
        receiver = accountRepository.save(receiver);
        
        TransferRequest request = new TransferRequest(
            sender.getId(),
            receiver.getId(),
            new BigDecimal("100.00")
        );
        
        // Create a transfer first
        ResponseEntity<TransferResponse> transferResponse = restTemplate.postForEntity(
            "/api/transfers",
            request,
            TransferResponse.class
        );
        
        Long transactionId = transferResponse.getBody().getTransactionId();
        
        // Act
        ResponseEntity<TransferResponse> statusResponse = restTemplate.getForEntity(
            "/api/transfers/" + transactionId,
            TransferResponse.class
        );
        
        // Assert
        assertEquals(HttpStatus.OK, statusResponse.getStatusCode());
        assertNotNull(statusResponse.getBody());
        assertEquals(transactionId, statusResponse.getBody().getTransactionId());
        assertEquals(TransactionStatus.COMPLETED, statusResponse.getBody().getStatus());
    }
}
