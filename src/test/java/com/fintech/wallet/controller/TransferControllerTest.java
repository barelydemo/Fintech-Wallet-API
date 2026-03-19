package com.fintech.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.wallet.dto.TransferRequest;
import com.fintech.wallet.dto.TransferResponse;
import com.fintech.wallet.entity.Transaction;
import com.fintech.wallet.entity.TransactionStatus;
import com.fintech.wallet.exception.AccountNotFoundException;
import com.fintech.wallet.exception.GlobalExceptionHandler;
import com.fintech.wallet.exception.InsufficientFundsException;
import com.fintech.wallet.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TransferController.
 * Tests controller behavior in isolation with mocked TransferService.
 * 
 * Validates: Requirements 7.1, 7.2, 7.3, 12.4
 */
@ExtendWith(MockitoExtension.class)
class TransferControllerTest {
    
    private MockMvc mockMvc;
    
    private ObjectMapper objectMapper;
    
    @Mock
    private TransferService transferService;
    
    @InjectMocks
    private TransferController transferController;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Register JavaTimeModule for LocalDateTime
        
        mockMvc = MockMvcBuilders.standaloneSetup(transferController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }
    
    /**
     * Test valid transfer request returns 200 OK with TransferResponse.
     * 
     * Validates: Requirements 7.1, 7.2, 7.3
     */
    @Test
    void testTransfer_ValidRequest_Returns200() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("100.00"));
        TransferResponse mockResponse = new TransferResponse(
            123L,
            TransactionStatus.COMPLETED,
            "Transfer successful",
            LocalDateTime.now()
        );
        
        when(transferService.transferMoney(eq(1L), eq(2L), any(BigDecimal.class)))
            .thenReturn(mockResponse);
        
        // Act & Assert
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.transactionId").value(123))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.message").value("Transfer successful"))
            .andExpect(jsonPath("$.timestamp").exists());
    }
    
    /**
     * Test missing senderId returns 400 Bad Request.
     * 
     * Validates: Requirements 12.4
     */
    @Test
    void testTransfer_MissingSenderId_Returns400() throws Exception {
        // Arrange
        String requestJson = "{\"receiverId\": 2, \"amount\": 100.00}";
        
        // Act & Assert
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Test missing receiverId returns 400 Bad Request.
     * 
     * Validates: Requirements 12.4
     */
    @Test
    void testTransfer_MissingReceiverId_Returns400() throws Exception {
        // Arrange
        String requestJson = "{\"senderId\": 1, \"amount\": 100.00}";
        
        // Act & Assert
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Test missing amount returns 400 Bad Request.
     * 
     * Validates: Requirements 12.4
     */
    @Test
    void testTransfer_MissingAmount_Returns400() throws Exception {
        // Arrange
        String requestJson = "{\"senderId\": 1, \"receiverId\": 2}";
        
        // Act & Assert
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Test negative senderId returns 400 Bad Request.
     * 
     * Validates: Requirements 12.4
     */
    @Test
    void testTransfer_NegativeSenderId_Returns400() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest(-1L, 2L, new BigDecimal("100.00"));
        
        // Act & Assert
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Test negative receiverId returns 400 Bad Request.
     * 
     * Validates: Requirements 12.4
     */
    @Test
    void testTransfer_NegativeReceiverId_Returns400() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest(1L, -2L, new BigDecimal("100.00"));
        
        // Act & Assert
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Test amount below minimum returns 400 Bad Request.
     * 
     * Validates: Requirements 12.4
     */
    @Test
    void testTransfer_AmountBelowMinimum_Returns400() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("0.001"));
        
        // Act & Assert
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Test malformed JSON returns 500 Internal Server Error.
     * Malformed JSON is a parsing error handled by the generic exception handler.
     * 
     * Validates: Requirements 12.4
     */
    @Test
    void testTransfer_MalformedJson_Returns500() throws Exception {
        // Arrange
        String malformedJson = "{\"senderId\": 1, \"receiverId\": 2, \"amount\": }";
        
        // Act & Assert
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
            .andExpect(status().isInternalServerError());
    }
    
    /**
     * Test insufficient funds exception returns 400 Bad Request.
     * 
     * Validates: Requirements 7.1, 7.2
     */
    @Test
    void testTransfer_InsufficientFunds_Returns400() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("1000.00"));
        
        when(transferService.transferMoney(eq(1L), eq(2L), any(BigDecimal.class)))
            .thenThrow(new InsufficientFundsException("Insufficient funds"));
        
        // Act & Assert
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Test account not found exception returns 404 Not Found.
     * 
     * Validates: Requirements 7.1, 7.2
     */
    @Test
    void testTransfer_AccountNotFound_Returns404() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest(1L, 999L, new BigDecimal("100.00"));
        
        when(transferService.transferMoney(eq(1L), eq(999L), any(BigDecimal.class)))
            .thenThrow(new AccountNotFoundException("Receiver account not found"));
        
        // Act & Assert
        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }
    
    /**
     * Test getTransferStatus with valid transaction ID returns 200 OK.
     * 
     * Validates: Requirements 7.4
     */
    @Test
    void testGetTransferStatus_ValidId_Returns200() throws Exception {
        // Arrange
        Long transactionId = 123L;
        Transaction mockTransaction = new Transaction(
            transactionId,
            1L,
            2L,
            new BigDecimal("100.00"),
            TransactionStatus.COMPLETED,
            LocalDateTime.now()
        );
        
        when(transferService.getTransactionById(eq(transactionId)))
            .thenReturn(mockTransaction);
        
        // Act & Assert
        mockMvc.perform(get("/api/transfers/{transactionId}", transactionId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.transactionId").value(123))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.message").value("Transfer status retrieved"))
            .andExpect(jsonPath("$.timestamp").exists());
    }
    
    /**
     * Test getTransferStatus with non-existent transaction ID returns 404 Not Found.
     * 
     * Validates: Requirements 7.4
     */
    @Test
    void testGetTransferStatus_NonExistentId_Returns404() throws Exception {
        // Arrange
        Long transactionId = 999L;
        
        when(transferService.getTransactionById(eq(transactionId)))
            .thenThrow(new AccountNotFoundException("Transaction not found"));
        
        // Act & Assert
        mockMvc.perform(get("/api/transfers/{transactionId}", transactionId))
            .andExpect(status().isNotFound());
    }
}
