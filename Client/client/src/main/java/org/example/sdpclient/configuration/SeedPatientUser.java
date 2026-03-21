package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.repository.PatientRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class SeedPatientUser {

    @Bean
    CommandLineRunner seedPatients(PatientRepository repo, PasswordEncoder encoder) {
        return args -> {
            seed(repo, encoder, "testPatient1", "testPatient1",
                    "Test", "Patient One",
                    LocalDate.of(1990, 1, 1),
                    "testpatient1@sdp.com", "07700 800001");

            seed(repo, encoder, "testPatient2", "testPatient2",
                    "Test", "Patient Two",
                    LocalDate.of(1990, 1, 2),
                    "testpatient2@sdp.com", "07700 800002");
        };
    }

    private void seed(PatientRepository repo,
                      PasswordEncoder encoder,
                      String username,
                      String rawPassword,
                      String firstName,
                      String lastName,
                      LocalDate dateOfBirth,
                      String email,
                      String phone) {

        if (repo.findByUsername(username).isPresent()) return;

        Patient patient = new Patient();
        patient.setUsername(username);
        patient.setPasswordHash(encoder.encode(rawPassword));
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setDateOfBirth(dateOfBirth);
        patient.setEmail(email);
        patient.setPhone(phone);

        repo.save(patient);
    }
}
