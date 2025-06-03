package com.example.productmanager.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class DatabaseInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @PostConstruct
    public void initialize() {
        logger.info("Initializing database triggers for inventory management");
        
        try {
            // Read the SQL file with trigger definitions
            ClassPathResource resource = new ClassPathResource("db/add_inventory_triggers.sql");
            InputStream inputStream = resource.getInputStream();
            String sql = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            
            // Split SQL by delimiter to handle multiple statements
            String[] statements = sql.split(";");
            
            // Execute each SQL statement
            for (String statement : statements) {
                statement = statement.trim();
                if (!statement.isEmpty() && !statement.contains("DELIMITER")) {
                    try {
                        logger.debug("Executing SQL: {}", statement);
                        jdbcTemplate.execute(statement);
                    } catch (Exception e) {
                        logger.error("Error executing SQL statement: {}", statement, e);
                    }
                }
            }
            
            logger.info("Database triggers for inventory management initialized successfully");
            
            // Verify that triggers exist
            verifyTriggers();
            
        } catch (IOException e) {
            logger.error("Error reading SQL file for database initialization", e);
        }
    }
    
    private void verifyTriggers() {
        String[] triggerNames = {
            "before_order_item_insert",
            "after_order_item_insert",
            "after_order_item_update",
            "after_order_item_delete"
        };
        
        for (String triggerName : triggerNames) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name = ?",
                    Integer.class,
                    triggerName
                );
                
                if (count != null && count > 0) {
                    logger.info("Trigger '{}' exists in the database", triggerName);
                } else {
                    logger.warn("Trigger '{}' does not exist in the database", triggerName);
                }
            } catch (Exception e) {
                logger.error("Error verifying trigger '{}'", triggerName, e);
            }
        }
    }
} 