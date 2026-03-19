package com.fintech.wallet.exception;

/**
 * Exception thrown when a requested account does not exist in the system.
 * 
 * This exception is thrown by the TransferService when either the sender or
 * receiver account cannot be found in the database.
 */
public class AccountNotFoundException extends RuntimeException {
    
    /**
     * Constructs a new AccountNotFoundException with the specified error message.
     * 
     * @param message the detail message explaining which account was not found
     */
    public AccountNotFoundException(String message) {
        super(message);
    }
}
