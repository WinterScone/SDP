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
        verify(repo, times(2)).save(captor.capture());

        var saved = captor.getAllValues();
        assertThat(saved).hasSize(2);

        // check fields + password behavior
        assertThat(saved.get(0).getUsername()).isEqualTo("testPatient1");
        assertThat(saved.get(0).getFirstName()).isEqualTo("Test");
        assertThat(saved.get(0).getLastName()).isEqualTo("Patient One");
        assertThat(saved.get(0).getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));

        String hash1 = saved.get(0).getPasswordHash();
        assertThat(hash1).isNotBlank();
        assertThat(hash1).doesNotContain("testPatient1");
        assertThat(encoder.matches("testPatient1", hash1)).isTrue();

        assertThat(saved.get(1).getUsername()).isEqualTo("testPatient2");
        assertThat(saved.get(1).getFirstName()).isEqualTo("Test");
        assertThat(saved.get(1).getLastName()).isEqualTo("Patient Two");
        assertThat(saved.get(1).getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 2));
    }

    @Test
    void seedPatients_skipsExisting_users_individually() throws Exception {
        PatientRepository repo = mock(PatientRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        // testPatient1 exists, testPatient2 does not
        when(repo.findByUsername("testPatient1")).thenReturn(Optional.of(new Patient()));
        when(repo.findByUsername("testPatient2")).thenReturn(Optional.empty());

        when(encoder.encode(anyString())).thenReturn("ENC(testPatient2)");

        SeedPatientUser config = new SeedPatientUser();
        CommandLineRunner runner = config.seedPatients(repo, encoder);

        runner.run();

        // should only encode/save for 1 user (testPatient2)
        verify(encoder, times(1)).encode("testPatient2");
        verify(repo, times(1)).save(any(Patient.class));
    }

    @Test
    void seedPatients_skipsAll_whenAllExist() throws Exception {
        PatientRepository repo = mock(PatientRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        when(repo.findByUsername(anyString())).thenReturn(Optional.of(new Patient()));

        SeedPatientUser config = new SeedPatientUser();
        CommandLineRunner runner = config.seedPatients(repo, encoder);

        runner.run();

        verify(repo, times(2)).findByUsername(anyString());
        verifyNoInteractions(encoder);
        verify(repo, never()).save(any(Patient.class));
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

