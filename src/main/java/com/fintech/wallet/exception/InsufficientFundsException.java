package com.fintech.wallet.exception;

/**
 * Exception thrown when a transfer cannot be completed due to insufficient funds
 * in the sender's account.
 * 
 * This exception is thrown by the TransferService when the sender's account balance
 * is less than the requested transfer amount.
 */
public class InsufficientFundsException extends RuntimeException {
    
    /**
     * Constructs a new InsufficientFundsException with the specified error message.
     * 
     * @param message the detail message explaining the reason for the exception
     */
    public InsufficientFundsException(String message) {
        super(message);
    }
}
