package org.example.sdpclient.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class ReminderStatusMigration {

    private static final Logger log = LoggerFactory.getLogger(ReminderStatusMigration.class);

    @Bean
    @Order(1)
    CommandLineRunner migrateReminderStatus(DataSource dataSource) {
        return args -> {
            try (Connection conn = dataSource.getConnection()) {
                String dbProduct = conn.getMetaData().getDatabaseProductName().toLowerCase();
                if (dbProduct.contains("h2")) {
                    log.info("Reminder status migration: H2 detected — skipping (fresh schema)");
                    return;
                }

                try (var stmt = conn.createStatement()) {
                    int taken = stmt.executeUpdate(
                            "UPDATE reminder_log SET status = 'COLLECTED' WHERE status = 'TAKEN'");
                    int failedSkipped = stmt.executeUpdate(
                            "UPDATE reminder_log SET status = 'MISSED' WHERE status IN ('FAILED', 'SKIPPED')");

                    if (taken + failedSkipped > 0) {
                        log.info("Reminder status migration: remapped {} TAKEN→COLLECTED, {} FAILED/SKIPPED→MISSED",
                                taken, failedSkipped);
                    } else {
                        log.info("Reminder status migration: no legacy values found — nothing to do");
                    }
                }
            }
        };
    }
}
