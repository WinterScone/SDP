package org.example.sdpclient.configuration;

import org.example.sdpclient.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SeedAdminUserTest {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seedAdmin_createsAdminWhenNotExists() {
        var admin = adminRepository.findByUsername("admin");

        assertTrue(admin.isPresent());
        assertEquals("admin", admin.get().getUsername());
        assertEquals("Admin", admin.get().getFirstName());
        assertEquals("User", admin.get().getLastName());
    }

    @Test
    void seedAdmin_passwordIsEncoded() {
        // findByUsername("admin") -> get passwordHash
        // assert passwordHash is NOT equal to "admin123"
        // assert passwordEncoder.matches("admin123", passwordHash) is true
    }

    @Test
    void seedAdmin_doesNotDuplicateOnRestart() {
        // findAllByUsername or count how many admins have username "admin"
        // assert exactly 1
    }
}
