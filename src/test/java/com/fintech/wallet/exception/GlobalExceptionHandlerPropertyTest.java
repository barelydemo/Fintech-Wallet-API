package com.fintech.wallet.exception;

import com.fintech.wallet.dto.ErrorResponse;
import net.jqwik.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for GlobalExceptionHandler.
 * 
 * **Validates: Requirements 8.1, 8.2, 8.3**
 * 
 * These tests validate that exception handling produces correct HTTP status codes
 * and consistent error response structures across many random scenarios.
 */
class GlobalExceptionHandlerPropertyTest {
    
    private GlobalExceptionHandler createHandler() {
        return new GlobalExceptionHandler();
    }
    
    /**
     * Property 11: Error Response Mapping - InsufficientFundsException
     * 
     * **Validates: Requirements 8.1**
     * 
     * This property verifies that InsufficientFundsException always maps to HTTP 400
     * and produces a consistent ErrorResponse structure.
     */
    @Property(tries = 50)
    void insufficientFundsExceptionMapsTo400(
        @ForAll("errorMessages") String errorMessage
    ) {
        // Arrange: Create handler and exception with random message
        GlobalExceptionHandler exceptionHandler = createHandler();
        InsufficientFundsException exception = new InsufficientFundsException(errorMessage);
        
        // Act: Handle exception
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInsufficientFunds(exception);
        
        // Assert: Verify HTTP 400 status
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), 
            "InsufficientFundsException must map to HTTP 400");
        
        // Assert: Verify ErrorResponse structure
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse, "ErrorResponse body should not be null");
        assertEquals("Insufficient Funds", errorResponse.getError(), 
            "Error field should be 'Insufficient Funds'");
        assertEquals(errorMessage, errorResponse.getMessage(), 
            "Message should match exception message");
        assertNotNull(errorResponse.getTimestamp(), 
            "Timestamp should not be null");
        assertTrue(errorResponse.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)), 
            "Timestamp should be recent");
    }
    
    /**
     * Property 11: Error Response Mapping - AccountNotFoundException
     * 
     * **Validates: Requirements 8.2**
     * 
     * This property verifies that AccountNotFoundException always maps to HTTP 404
     * and produces a consistent ErrorResponse structure.
     */
    @Property(tries = 50)
    void accountNotFoundExceptionMapsTo404(
        @ForAll("errorMessages") String errorMessage
    ) {
        // Arrange: Create handler and exception with random message
        GlobalExceptionHandler exceptionHandler = createHandler();
        AccountNotFoundException exception = new AccountNotFoundException(errorMessage);
        
        // Act: Handle exception
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccountNotFound(exception);
        
        // Assert: Verify HTTP 404 status
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), 
            "AccountNotFoundException must map to HTTP 404");
        
        // Assert: Verify ErrorResponse structure
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse, "ErrorResponse body should not be null");
        assertEquals("Account Not Found", errorResponse.getError(), 
            "Error field should be 'Account Not Found'");
        assertEquals(errorMessage, errorResponse.getMessage(), 
            "Message should match exception message");
        assertNotNull(errorResponse.getTimestamp(), 
            "Timestamp should not be null");
        assertTrue(errorResponse.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)), 
            "Timestamp should be recent");
    }
    
    /**
     * Property 11: Error Response Mapping - IllegalArgumentException
     * 
     * This property verifies that IllegalArgumentException maps to HTTP 400
     * and produces a consistent ErrorResponse structure.
     */
    @Property(tries = 50)
    void illegalArgumentExceptionMapsTo400(
        @ForAll("errorMessages") String errorMessage
    ) {
        // Arrange: Create handler and exception with random message
        GlobalExceptionHandler exceptionHandler = createHandler();
        IllegalArgumentException exception = new IllegalArgumentException(errorMessage);
        
        // Act: Handle exception
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(exception);
        
        // Assert: Verify HTTP 400 status
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), 
            "IllegalArgumentException must map to HTTP 400");
        
        // Assert: Verify ErrorResponse structure
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse, "ErrorResponse body should not be null");
        assertEquals("Invalid Request", errorResponse.getError(), 
            "Error field should be 'Invalid Request'");
        assertEquals(errorMessage, errorResponse.getMessage(), 
            "Message should match exception message");
        assertNotNull(errorResponse.getTimestamp(), 
            "Timestamp should not be null");
        assertTrue(errorResponse.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)), 
            "Timestamp should be recent");
    }
    
    /**
     * Property 11: Error Response Mapping - Generic Exception
     * 
     * **Validates: Requirements 8.3**
     * 
     * This property verifies that generic exceptions map to HTTP 500
     * and produce a consistent ErrorResponse structure with a generic message.
     */
    @Property(tries = 50)
    void genericExceptionMapsTo500(
        @ForAll("errorMessages") String errorMessage
    ) {
        // Arrange: Create handler and generic exception with random message
        GlobalExceptionHandler exceptionHandler = createHandler();
        Exception exception = new RuntimeException(errorMessage);
        
        // Act: Handle exception
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);
        
        // Assert: Verify HTTP 500 status
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), 
            "Generic exceptions must map to HTTP 500");
        
        // Assert: Verify ErrorResponse structure
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse, "ErrorResponse body should not be null");
        assertEquals("Internal Server Error", errorResponse.getError(), 
            "Error field should be 'Internal Server Error'");
        assertEquals("An unexpected error occurred. Please try again later.", 
            errorResponse.getMessage(), 
            "Message should be generic for security reasons");
        assertNotNull(errorResponse.getTimestamp(), 
            "Timestamp should not be null");
        assertTrue(errorResponse.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)), 
            "Timestamp should be recent");
    }
    
    /**
     * Property 11: Error Response Structure Consistency
     * 
     * **Validates: Requirements 8.1, 8.2, 8.3**
     * 
     * This property verifies that all error responses have consistent structure
     * regardless of exception type: error field, message field, and timestamp field.
     */
    @Property(tries = 50)
    void errorResponseStructureIsConsistent(
        @ForAll("exceptionTypes") ExceptionType exceptionType,
        @ForAll("errorMessages") String errorMessage
    ) {
        // Arrange: Create handler and exception based on type
        GlobalExceptionHandler exceptionHandler = createHandler();
        Exception exception = createException(exceptionType, errorMessage);
        
        // Act: Handle exception
        ResponseEntity<ErrorResponse> response = handleException(exceptionHandler, exceptionType, exception);
        
        // Assert: Verify response structure consistency
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getBody(), "Response body should not be null");
        
        ErrorResponse errorResponse = response.getBody();
        
        // Verify all required fields are present
        assertNotNull(errorResponse.getError(), "Error field must be present");
        assertFalse(errorResponse.getError().isEmpty(), "Error field must not be empty");
        
        assertNotNull(errorResponse.getMessage(), "Message field must be present");
        assertFalse(errorResponse.getMessage().isEmpty(), "Message field must not be empty");
        
        assertNotNull(errorResponse.getTimestamp(), "Timestamp field must be present");
        assertTrue(errorResponse.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)), 
            "Timestamp should be recent");
        
        // Verify HTTP status code matches exception type
        HttpStatus expectedStatus = getExpectedStatus(exceptionType);
        assertEquals(expectedStatus, response.getStatusCode(), 
            "HTTP status must match exception type");
    }
    
    // Helper methods
    
    private Exception createException(ExceptionType type, String message) {
        return switch (type) {
            case INSUFFICIENT_FUNDS -> new InsufficientFundsException(message);
            case ACCOUNT_NOT_FOUND -> new AccountNotFoundException(message);
            case ILLEGAL_ARGUMENT -> new IllegalArgumentException(message);
            case GENERIC -> new RuntimeException(message);
        };
    }
    
    private ResponseEntity<ErrorResponse> handleException(GlobalExceptionHandler handler, ExceptionType type, Exception exception) {
        return switch (type) {
            case INSUFFICIENT_FUNDS -> handler.handleInsufficientFunds((InsufficientFundsException) exception);
            case ACCOUNT_NOT_FOUND -> handler.handleAccountNotFound((AccountNotFoundException) exception);
            case ILLEGAL_ARGUMENT -> handler.handleIllegalArgument((IllegalArgumentException) exception);
            case GENERIC -> handler.handleGenericException(exception);
        };
    }
    
    private HttpStatus getExpectedStatus(ExceptionType type) {
        return switch (type) {
            case INSUFFICIENT_FUNDS, ILLEGAL_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case ACCOUNT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case GENERIC -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    // Custom arbitraries
    
    @Provide
    Arbitrary<String> errorMessages() {
        return Arbitraries.oneOf(
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(100),
            Arbitraries.of(
                "Account 123 not found",
                "Insufficient funds in account 456",
                "Transfer amount must be positive",
                "Sender account does not exist",
                "Receiver account does not exist",
                "Account balance is too low",
                "Invalid transfer amount",
                "Database connection failed",
                "Unexpected error occurred"
            )
        );
    }
    
    @Provide
    Arbitrary<ExceptionType> exceptionTypes() {
        return Arbitraries.of(ExceptionType.values());
    }
    
    // Enum for exception types
    enum ExceptionType {
        INSUFFICIENT_FUNDS,
        ACCOUNT_NOT_FOUND,
        ILLEGAL_ARGUMENT,
        GENERIC
    }
}
