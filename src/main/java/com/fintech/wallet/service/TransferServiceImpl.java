package com.fintech.wallet.service;

import com.fintech.wallet.dto.TransferResponse;
import com.fintech.wallet.entity.Account;
import com.fintech.wallet.entity.Transaction;
import com.fintech.wallet.entity.TransactionStatus;
import com.fintech.wallet.exception.AccountNotFoundException;
import com.fintech.wallet.exception.InsufficientFundsException;
import com.fintech.wallet.repository.AccountRepository;
import com.fintech.wallet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Implementation of TransferService with transaction management and pessimistic locking.
 * 
 * Validates: Requirements 2.1, 11.4, 1.1, 1.2, 3.1, 4.1, 4.2, 5.1, 2.3, 6.1, 6.2, 6.3
 */
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 30)
    public TransferResponse transferMoney(Long senderId, Long receiverId, BigDecimal amount) {
        // Validate amount > 0
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        // Lock sender account
        Account sender = accountRepository.findByIdWithLock(senderId)
            .orElseThrow(() -> new AccountNotFoundException(
                String.format("Sender account %d not found", senderId)
            ));
        
        // Lock receiver account
        Account receiver = accountRepository.findByIdWithLock(receiverId)
            .orElseThrow(() -> new AccountNotFoundException(
                String.format("Receiver account %d not found", receiverId)
            ));
        
        // Validate sender balance >= amount
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                String.format("Account %d has insufficient balance. Available: %s, Required: %s",
                    senderId, sender.getBalance(), amount)
            );
        }
        
        // Debit sender account
        sender.setBalance(sender.getBalance().subtract(amount));
        
        // Credit receiver account
        receiver.setBalance(receiver.getBalance().add(amount));
        
        // Save both accounts
        accountRepository.save(sender);
        accountRepository.save(receiver);
        
        // Create transaction record with status COMPLETED
        Transaction transaction = new Transaction();
        transaction.setSenderId(senderId);
        transaction.setReceiverId(receiverId);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setTimestamp(LocalDateTime.now());
        
        transactionRepository.save(transaction);
        
        // Return TransferResponse
        return new TransferResponse(
            transaction.getId(),
            TransactionStatus.COMPLETED,
            "Transfer successful",
            transaction.getTimestamp()
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public Transaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId)
            .orElseThrow(() -> new AccountNotFoundException(
                String.format("Transaction %d not found", transactionId)
            ));
    }
}
