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
        var root = adminRepository.findByUsername("root");
        assertTrue(root.isPresent());
        assertEquals("Root", root.get().getFirstName());
        assertEquals("Admin", root.get().getLastName());

        var admin1 = adminRepository.findByUsername("testAdmin1");
        assertTrue(admin1.isPresent());
        assertEquals("Louis", admin1.get().getFirstName());
        assertEquals("Admin One", admin1.get().getLastName());

        var admin2 = adminRepository.findByUsername("testAdmin2");
        assertTrue(admin2.isPresent());
        assertEquals("Ting", admin2.get().getFirstName());
        assertEquals("Admin Two", admin2.get().getLastName());
    }

    @Test
    void seedAdmin_passwordIsEncoded() {
        var usernames = java.util.List.of("root", "testAdmin1", "testAdmin2");

        for (String username : usernames) {
            var admin = adminRepository.findByUsername(username).orElseThrow();
            assertNotEquals(username, admin.getPasswordHash());
            assertTrue(passwordEncoder.matches(username, admin.getPasswordHash()));
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
