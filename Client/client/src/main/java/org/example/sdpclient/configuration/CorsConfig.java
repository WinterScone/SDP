package org.example.sdpclient.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // The frontend is served from the same origin (resources/static),
                // so CORS is not needed for normal operation. This config exists
                // to allow cross-origin tools (Postman, external test clients).
                // Cookies are same-origin only; cross-origin clients must use
                // other auth mechanisms.
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("*")
                        .allowCredentials(false);
            }
        };
    }
}
