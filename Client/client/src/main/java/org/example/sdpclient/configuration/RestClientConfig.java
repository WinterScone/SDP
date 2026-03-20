package org.example.sdpclient.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${python.service.url}")
    private String pythonServiceUrl;

    @Bean
    public RestClient pythonRestClient() {
        return RestClient.builder()
                .baseUrl(pythonServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
