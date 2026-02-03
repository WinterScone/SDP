package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.repository.PatientRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
public class SeedPatientUser {

    @Bean
    CommandLineRunner seedPatients(PatientRepository repo, PasswordEncoder encoder) {
        return args -> {
            seed(repo, encoder, "patient1", "patient123",
                    "Test", "Patient",
                    LocalDate.of(2000, 1, 1).toString());

            seed(repo, encoder, "patient2", "patient123",
                    "Jane", "Doe",
                    LocalDate.of(1995, 5, 20).toString());

            seed(repo, encoder, "patient3", "patient123",
                    "John", "Smith",
                    LocalDate.of(1988, 11, 3).toString());
        };
    }

    private void seed(PatientRepository repo,
                      PasswordEncoder encoder,
                      String username,
                      String rawPassword,
                      String firstName,
                      String lastName,
                      String dateOfBirth) {

        if (repo.findByUsername(username).isPresent()) return;

        Patient patient = new Patient();
        patient.setUsername(username);
        patient.setPasswordHash(encoder.encode(rawPassword));
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setDateOfBirth(dateOfBirth);

        repo.save(patient);
    }
}



