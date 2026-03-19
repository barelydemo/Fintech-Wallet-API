package com.fintech.wallet.exception;

import com.fintech.wallet.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GlobalExceptionHandler.
 * 
 * Validates: Requirements 8.1, 8.2, 8.3
 */
class GlobalExceptionHandlerTest {
    
    private GlobalExceptionHandler exceptionHandler;
    
    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }
    
    /**
     * Test InsufficientFundsException returns HTTP 400 with error message.
     * 
     * Validates: Requirements 8.1
     */
    @Test
    void testHandleInsufficientFunds() {
        // Arrange
        String errorMessage = "Account 123 has insufficient balance for transfer";
        InsufficientFundsException exception = new InsufficientFundsException(errorMessage);
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInsufficientFunds(exception);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("Insufficient Funds", errorResponse.getError());
        assertEquals(errorMessage, errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }
    
    /**
     * Test AccountNotFoundException returns HTTP 404 with error message.
     * 
     * Validates: Requirements 8.2
     */
    @Test
    void testHandleAccountNotFound() {
        // Arrange
        String errorMessage = "Sender account 999 not found";
        AccountNotFoundException exception = new AccountNotFoundException(errorMessage);
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccountNotFound(exception);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("Account Not Found", errorResponse.getError());
        assertEquals(errorMessage, errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }
    
    /**
     * Test generic Exception returns HTTP 500 with generic message.
     * 
     * Validates: Requirements 8.3
     */
    @Test
    void testHandleGenericException() {
        // Arrange
        String internalErrorMessage = "Database connection failed";
        Exception exception = new Exception(internalErrorMessage);
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("Internal Server Error", errorResponse.getError());
        assertEquals("An unexpected error occurred. Please try again later.", errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }
}
