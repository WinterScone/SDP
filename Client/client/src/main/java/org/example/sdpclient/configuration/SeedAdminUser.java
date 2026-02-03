package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Admin;
import org.example.sdpclient.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedAdminUser {

    @Bean
    CommandLineRunner seedAdmin(AdminRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.findByUsername("admin").isEmpty()) {
                Admin admin = new Admin();
                admin.setUsername("admin");
                admin.setPasswordHash(encoder.encode("admin123"));
                admin.setFirstName("Admin");
                admin.setLastName("User");
                repo.save(admin);
            }
        };
    }
}
