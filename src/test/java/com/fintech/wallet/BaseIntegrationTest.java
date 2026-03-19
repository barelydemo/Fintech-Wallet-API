package com.fintech.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fintech.wallet.repository.AccountRepository;
import com.fintech.wallet.repository.TransactionRepository;

/**
 * Base class for integration tests using Testcontainers with MySQL.
 * 
 * This class provides:
 * - MySQL container configuration for realistic database testing
 * - Spring Boot test context with random port
 * - Automatic cleanup between tests
 * 
 * Validates: Requirements 11.1, 11.2, 11.3
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class BaseIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);
    
    @Autowired
    protected AccountRepository accountRepository;
    
    @Autowired
    protected TransactionRepository transactionRepository;
    
    /**
     * Configure Spring Boot to use the Testcontainers MySQL instance.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        
        // JPA/Hibernate configuration for MySQL
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.show-sql", () -> "false");
    }
    
    /**
     * Clean up database state before each test to ensure test isolation.
     */
    @BeforeEach
    void cleanupDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }
}
