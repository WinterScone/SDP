package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SeedPatientUserTest {

    @Test
    void seedPatients_insertsThree_whenNoneExist() throws Exception {
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

        // check fields + password behavior
        assertThat(saved.get(0).getUsername()).isEqualTo("patient1");
        assertThat(saved.get(0).getFirstName()).isEqualTo("Test");
        assertThat(saved.get(0).getLastName()).isEqualTo("Patient");
        assertThat(saved.get(0).getDateOfBirth()).isEqualTo("2000-01-01");

        String hash1 = saved.get(0).getPasswordHash();
        assertThat(hash1).isNotBlank();
        assertThat(hash1).doesNotContain("patient123");
        assertThat(encoder.matches("patient123", hash1)).isTrue();

        // you can similarly check patient2/patient3 if you want
    }

    @Test
    void seedPatients_skipsExisting_users_individually() throws Exception {
        PatientRepository repo = mock(PatientRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        // patient1 exists, others do not
        when(repo.findByUsername("patient1")).thenReturn(Optional.of(new Patient()));
        when(repo.findByUsername("patient2")).thenReturn(Optional.empty());
        when(repo.findByUsername("patient3")).thenReturn(Optional.empty());

        when(encoder.encode(anyString())).thenReturn("ENC(patient123)");

        SeedPatientUser config = new SeedPatientUser();
        CommandLineRunner runner = config.seedPatients(repo, encoder);

        runner.run();

        // should only encode/save for 2 users (patient2 and patient3)
        verify(encoder, times(2)).encode("patient123");
        verify(repo, times(2)).save(any(Patient.class));
    }

    @Test
    void seedPatients_skipsAll_whenAllExist() throws Exception {
        PatientRepository repo = mock(PatientRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        when(repo.findByUsername(anyString())).thenReturn(Optional.of(new Patient()));

        SeedPatientUser config = new SeedPatientUser();
        CommandLineRunner runner = config.seedPatients(repo, encoder);

        runner.run();

        verify(repo, times(3)).findByUsername(anyString());
        verifyNoInteractions(encoder);
        verify(repo, never()).save(any(Patient.class));
    }

    private static void assertPatient(
            Patient p,
            String username,
            String passwordHash,
            String firstName,
            String lastName,
            String dob
    ) {
        assertThat(p.getUsername()).isEqualTo(username);
        assertThat(p.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(p.getFirstName()).isEqualTo(firstName);
        assertThat(p.getLastName()).isEqualTo(lastName);
        assertThat(p.getDateOfBirth()).isEqualTo(dob);
        // also verify raw password isn't accidentally stored
        assertThat(p.getPasswordHash()).doesNotContain("patient123");
    }
}

