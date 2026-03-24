package org.example.sdpclient.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Configuration
public class FrequencyColumnMigration {

    private static final Logger log = LoggerFactory.getLogger(FrequencyColumnMigration.class);

    @Bean
    @Order(0)
    CommandLineRunner migrateFrequencyColumn(DataSource dataSource) {
        return args -> {
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet rs = meta.getColumns(null, null, "prescription", "frequency")) {
                    if (!rs.next()) {
                        log.info("Frequency column not found — skipping migration");
                        return;
                    }
                    String typeName = rs.getString("TYPE_NAME").toLowerCase();
                    if (!typeName.contains("varchar") && !typeName.contains("character varying") && !typeName.contains("text")) {
                        log.info("Frequency column is already type '{}' — skipping migration", typeName);
                        return;
                    }
                }

                log.info("Migrating frequency column from varchar to integer...");

                try (var stmt = conn.createStatement()) {
                    stmt.executeUpdate("UPDATE prescription SET frequency = '1' WHERE frequency = 'ONCE_A_DAY'");
                    stmt.executeUpdate("UPDATE prescription SET frequency = '2' WHERE frequency = 'TWICE_A_DAY'");
                    stmt.executeUpdate("UPDATE prescription SET frequency = '3' WHERE frequency = 'THREE_TIMES_A_DAY'");
                    stmt.executeUpdate("UPDATE prescription SET frequency = '4' WHERE frequency = 'FOUR_TIMES_A_DAY'");
                    // fallback: any unrecognized value defaults to 1
                    stmt.executeUpdate("UPDATE prescription SET frequency = '1' WHERE frequency !~ '^\\d+$'");
                    stmt.executeUpdate("ALTER TABLE prescription ALTER COLUMN frequency TYPE integer USING frequency::integer");
                }

                log.info("Frequency column migration complete");
            }
        };
    }
}
