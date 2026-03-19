package com.fintech.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for the FinTech Wallet API.
 * 
 * This application provides a high-concurrency peer-to-peer money transfer system
 * with pessimistic locking to prevent race conditions and ensure data consistency.
 */
@SpringBootApplication
@EnableJpaAuditing
public class WalletApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletApiApplication.class, args);
    }
}
