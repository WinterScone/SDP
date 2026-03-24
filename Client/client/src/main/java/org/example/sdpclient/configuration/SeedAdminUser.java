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
            seed(repo, encoder, "root",       "root",       "Root",  "Admin",     "root@sdp.com",       "+447761844142", true);
            seed(repo, encoder, "testAdmin1", "testAdmin1", "Louis",  "Admin One", "testadmin1@sdp.com", "+447751246179", false);
            seed(repo, encoder, "testAdmin2", "testAdmin2", "Ting",  "Admin Two", "testadmin2@sdp.com", "+447828163035", false);
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

        Admin admin = repo.findByUsername(username).orElse(new Admin());
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

