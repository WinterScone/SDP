package org.example.sdpclient.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CryptographyPassword.class)
class CryptographyPasswordConfigTest {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void passwordEncoderBean_isCreated() {
        assertThat(passwordEncoder).isNotNull();
    }

    @Test
    void passwordEncoder_isBCrypt() {
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoder_encodesAndMatchesPassword() {
        String raw = "super-secret";

        String encoded = passwordEncoder.encode(raw);

        assertThat(encoded).isNotEqualTo(raw);
        assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
    }

    @Test
    void passwordEncoder_doesNotMatchWrongPassword() {
        String encoded = passwordEncoder.encode("correct-password");

        assertThat(passwordEncoder.matches("wrong-password", encoded)).isFalse();
    }
}

