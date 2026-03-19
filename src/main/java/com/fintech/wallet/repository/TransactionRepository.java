package com.fintech.wallet.repository;

import com.fintech.wallet.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Transaction entity.
 * 
 * Validates: Requirements 6.2, 6.3
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Finds all transactions where the specified account is the sender.
     * 
     * @param senderId the sender account ID
     * @return list of transactions sent by the account
     */
    List<Transaction> findBySenderId(Long senderId);
    
    /**
     * Finds all transactions where the specified account is the receiver.
     * 
     * @param receiverId the receiver account ID
     * @return list of transactions received by the account
     */
    List<Transaction> findByReceiverId(Long receiverId);
}
