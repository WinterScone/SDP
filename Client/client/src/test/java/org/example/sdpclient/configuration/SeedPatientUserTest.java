package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SeedPatientUserTest {

    @Test
    void seedPatients_insertsTwo_whenNoneExist() throws Exception {
        PatientRepository repo = mock(PatientRepository.class);

        // real encoder
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        when(repo.findByUsername(anyString())).thenReturn(Optional.empty());

        SeedPatientUser config = new SeedPatientUser();
        CommandLineRunner runner = config.seedPatients(repo, encoder);

        runner.run(new String[0]);

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(repo, times(3)).save(captor.capture());

        var saved = captor.getAllValues();
        assertThat(saved).hasSize(3);

        // testPatient1
        assertThat(saved.get(0).getUsername()).isEqualTo("testPatient1");
        assertThat(saved.get(0).getFirstName()).isEqualTo("Asshmar");
        assertThat(saved.get(0).getLastName()).isEqualTo("Patient One");
        assertThat(saved.get(0).getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));

        // testPatient2
        assertThat(saved.get(1).getUsername()).isEqualTo("testPatient2");
        assertThat(saved.get(1).getFirstName()).isEqualTo("Angelo");
        assertThat(saved.get(1).getLastName()).isEqualTo("Patient Two");
        assertThat(saved.get(1).getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));

        String hash2 = saved.get(1).getPasswordHash();
        assertThat(hash2).isNotBlank();
        assertThat(hash2).doesNotContain("testPatient2");
        assertThat(encoder.matches("testPatient2", hash2)).isTrue();

        // testPatient3
        assertThat(saved.get(2).getUsername()).isEqualTo("testPatient3");
        assertThat(saved.get(2).getFirstName()).isEqualTo("Bahar");
        assertThat(saved.get(2).getLastName()).isEqualTo("Patient Three");
        assertThat(saved.get(2).getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 2));
    }

    @Test
    void seedPatients_upsertsExisting_users_individually() throws Exception {
        PatientRepository repo = mock(PatientRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        // testPatient1 exists, testPatient2 and testPatient3 do not
        when(repo.findByUsername("testPatient1")).thenReturn(Optional.of(new Patient()));
        when(repo.findByUsername("testPatient2")).thenReturn(Optional.empty());
        when(repo.findByUsername("testPatient3")).thenReturn(Optional.empty());

        when(encoder.encode(anyString())).thenReturn("ENC");

        SeedPatientUser config = new SeedPatientUser();
        CommandLineRunner runner = config.seedPatients(repo, encoder);

        runner.run();

        // upsert: encode and save all 3 regardless
        verify(encoder, times(3)).encode(anyString());
        verify(repo, times(3)).save(any(Patient.class));
    }

    @Test
    void seedPatients_upsertsAll_whenAllExist() throws Exception {
        PatientRepository repo = mock(PatientRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        when(repo.findByUsername(anyString())).thenReturn(Optional.of(new Patient()));
        when(encoder.encode(anyString())).thenReturn("ENC");

        SeedPatientUser config = new SeedPatientUser();
        CommandLineRunner runner = config.seedPatients(repo, encoder);

        runner.run();

        // upsert: always encode and save all 3
        verify(encoder, times(3)).encode(anyString());
        verify(repo, times(3)).save(any(Patient.class));
    }

    private static void assertPatient(
            Patient p,
            String username,
            String passwordHash,
            String firstName,
            String lastName,
            LocalDate dob
    ) {
        assertThat(p.getUsername()).isEqualTo(username);
        assertThat(p.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(p.getFirstName()).isEqualTo(firstName);
        assertThat(p.getLastName()).isEqualTo(lastName);
        assertThat(p.getDateOfBirth()).isEqualTo(dob);
    }
}

