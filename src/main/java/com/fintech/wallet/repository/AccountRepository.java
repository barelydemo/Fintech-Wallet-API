package com.fintech.wallet.repository;

import com.fintech.wallet.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Account entity with pessimistic locking support.
 * 
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    /**
     * Finds an account by ID with a pessimistic write lock.
     * This method acquires a database-level exclusive row lock (SELECT ... FOR UPDATE)
     * to prevent concurrent modifications during money transfers.
     * 
     * @param id the account ID
     * @return Optional containing the locked Account, or empty if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);
}
