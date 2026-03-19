package com.fintech.wallet.dto;

import com.fintech.wallet.entity.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    
    private Long transactionId;
    private TransactionStatus status;
    private String message;
    private LocalDateTime timestamp;
}
