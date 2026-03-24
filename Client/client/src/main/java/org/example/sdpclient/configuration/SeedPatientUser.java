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
                    "Asshmar", "Patient One",
                    LocalDate.of(1990, 1, 1),
                    "patient1@sdp.com", "+447444660738");

            seed(repo, encoder, "testPatient2", "testPatient2",
                    "Angelo", "Patient Two",
                    LocalDate.of(1990, 1, 1),
                    "testpatient1@sdp.com", "+447496151506");

            seed(repo, encoder, "testPatient3", "testPatient3",
                    "Bahar", "Patient Three",
                    LocalDate.of(1990, 1, 2),
                    "testpatient2@sdp.com", "+447398742784");
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

        Patient patient = repo.findByUsername(username).orElse(new Patient());
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
