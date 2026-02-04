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
    CommandLineRunner seedAdmins(AdminRepository repo, PasswordEncoder encoder) {
        return args -> {
            seed(repo, encoder, "root",   "root",     "Root",  "Root",  "root@admin.com",   "07700 900001", true);
            seed(repo, encoder, "admin1", "admin123", "Admin", "One",   "admin1@admin.com", "07700 900101", false);
            seed(repo, encoder, "admin2", "admin123", "Admin", "Two",   "admin2@admin.com", "07700 900102", false);
            seed(repo, encoder, "admin3", "admin123", "Admin", "Three", "admin3@admin.com", "07700 900103", false);
        };
    }

    private void seed(AdminRepository repo,
                      PasswordEncoder encoder,
                      String username,
                      String rawPassword,
                      String firstName,
                      String lastName,
                      String email,
                      String phone,
                      boolean root) {

        if (repo.findByUsername(username).isPresent()) return;

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPasswordHash(encoder.encode(rawPassword));
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        admin.setEmail(email);
        admin.setPhone(phone);
        admin.setRoot(root);

        repo.save(admin);
    }
}

