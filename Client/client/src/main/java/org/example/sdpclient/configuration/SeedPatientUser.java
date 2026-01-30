package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.repository.PatientRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class SeedPatientUser {


    @Bean
    CommandLineRunner seedPatient(PatientRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.findByUsername("patient1").isEmpty()) {
                Patient p = new Patient();
                p.setUsername("patient1");
                p.setPasswordHash(encoder.encode("patient123"));
                p.setFirstName("Test");
                p.setLastName("Patient");
                p.setDateOfBirth(LocalDateTime.of(2000, 1, 1, 0, 0).toString());
                repo.save(p);
            }
        };
    }
}

