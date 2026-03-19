package com.fintech.wallet.exception;

import com.fintech.wallet.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API.
 * 
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4, 12.4
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles InsufficientFundsException.
     * 
     * @param ex the exception
     * @return ResponseEntity with HTTP 400 and error details
     * 
     * Validates: Requirements 8.1
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex) {
        logger.error("Insufficient funds error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Insufficient Funds",
            ex.getMessage(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles AccountNotFoundException.
     * 
     * @param ex the exception
     * @return ResponseEntity with HTTP 404 and error details
     * 
     * Validates: Requirements 8.2
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        logger.error("Account not found error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Account Not Found",
            ex.getMessage(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handles validation errors from @Valid annotation.
     * 
     * @param ex the exception
     * @return ResponseEntity with HTTP 400 and validation error details
     * 
     * Validates: Requirements 12.4
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        logger.error("Validation error: {}", ex.getMessage());
        
        String errorMessage = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Validation Error",
            errorMessage,
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles IllegalArgumentException.
     * 
     * @param ex the exception
     * @return ResponseEntity with HTTP 400 and error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.error("Illegal argument error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Invalid Request",
            ex.getMessage(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles all other unexpected exceptions.
     * 
     * @param ex the exception
     * @return ResponseEntity with HTTP 500 and generic error message
     * 
     * Validates: Requirements 8.3, 8.4
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
