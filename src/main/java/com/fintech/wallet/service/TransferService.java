package com.fintech.wallet.service;

import com.fintech.wallet.dto.TransferResponse;
import com.fintech.wallet.entity.Transaction;

import java.math.BigDecimal;

/**
 * Service interface for money transfer operations.
 * 
 * Validates: Requirement 2.1
 */
public interface TransferService {
    
    /**
     * Transfers money from sender account to receiver account.
     * 
     * @param senderId the ID of the sender account
     * @param receiverId the ID of the receiver account
     * @param amount the amount to transfer
     * @return TransferResponse containing transaction details
     * @throws com.fintech.wallet.exception.AccountNotFoundException if sender or receiver account not found
     * @throws com.fintech.wallet.exception.InsufficientFundsException if sender has insufficient funds
     * @throws IllegalArgumentException if amount is invalid
     */
    TransferResponse transferMoney(Long senderId, Long receiverId, BigDecimal amount);
    
    /**
     * Retrieves a transaction by its ID.
     * 
     * @param transactionId the transaction ID
     * @return Transaction entity
     * @throws com.fintech.wallet.exception.AccountNotFoundException if transaction not found
     */
    Transaction getTransactionById(Long transactionId);
}
