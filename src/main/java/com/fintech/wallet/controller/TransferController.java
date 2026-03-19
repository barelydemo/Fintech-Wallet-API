package com.fintech.wallet.controller;

import com.fintech.wallet.dto.TransferRequest;
import com.fintech.wallet.dto.TransferResponse;
import com.fintech.wallet.entity.Transaction;
import com.fintech.wallet.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for money transfer operations.
 * 
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4
 */
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {
    
    private final TransferService transferService;
    
    /**
     * Initiates a money transfer between accounts.
     * 
     * @param request the transfer request containing senderId, receiverId, and amount
     * @return ResponseEntity containing TransferResponse with transaction details
     * 
     * Validates: Requirements 7.1, 7.2, 7.3
     */
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.transferMoney(
            request.getSenderId(),
            request.getReceiverId(),
            request.getAmount()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Retrieves the status of a transfer by transaction ID.
     * 
     * @param transactionId the ID of the transaction to retrieve
     * @return ResponseEntity containing TransferResponse with transaction details
     * 
     * Validates: Requirements 7.4
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransferResponse> getTransferStatus(@PathVariable Long transactionId) {
        Transaction transaction = transferService.getTransactionById(transactionId);
        
        TransferResponse response = new TransferResponse(
            transaction.getId(),
            transaction.getStatus(),
            "Transfer status retrieved",
            transaction.getTimestamp()
        );
        
        return ResponseEntity.ok(response);
    }
}
