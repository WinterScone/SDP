package org.example.sdpclient.configuration;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Parses Railway's DATABASE_URL (postgresql://user:pass@host:port/db) into a
 * JDBC DataSource. When DATABASE_URL is absent (local dev), falls back to the
 * spring.datasource.* defaults in application.properties.
 */
@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            URI uri = URI.create(databaseUrl);
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
            String userInfo = uri.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                properties.setUsername(parts[0]);
                properties.setPassword(parts[1]);
            }
            properties.setUrl(jdbcUrl);
        }
        return properties.initializeDataSourceBuilder().build();
    }
}
