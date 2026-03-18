package org.example.sdpclient.configuration;

import org.example.sdpclient.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SeedAdminUserTest {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Qualifier("seedAdmins")
    private CommandLineRunner seedAdmins;

    @Test
    void seedAdmin_createsAdminWhenNotExists() {
        var admin1 = adminRepository.findByUsername("admin1");
        assertTrue(admin1.isPresent());
        assertEquals("Admin", admin1.get().getFirstName());
        assertEquals("One", admin1.get().getLastName());

        var admin2 = adminRepository.findByUsername("admin2");
        assertTrue(admin2.isPresent());
        assertEquals("Admin", admin2.get().getFirstName());
        assertEquals("Two", admin2.get().getLastName());

        var admin3 = adminRepository.findByUsername("admin3");
        assertTrue(admin3.isPresent());
        assertEquals("Admin", admin3.get().getFirstName());
        assertEquals("Three", admin3.get().getLastName());
    }

    @Test
    void seedAdmin_passwordIsEncoded() {
        var usernames = java.util.List.of("admin1", "admin2", "admin3");

        for (String username : usernames) {
            var admin = adminRepository.findByUsername(username).orElseThrow();
            assertNotEquals("admin123", admin.getPasswordHash());
            assertTrue(passwordEncoder.matches("admin123", admin.getPasswordHash()));
        }
    }

    @Test
    void seedAdmin_doesNotDuplicateOnRestart() throws Exception {
        long countBefore = adminRepository.count();

        seedAdmins.run();

        long countAfter = adminRepository.count();
        assertEquals(countBefore, countAfter);
    }
}
