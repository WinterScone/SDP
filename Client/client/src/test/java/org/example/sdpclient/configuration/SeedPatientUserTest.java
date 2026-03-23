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
    void seedPatients_insertsTwo_whenNoneExist() throws Exception {
        PatientRepository repo = mock(PatientRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        when(repo.findByUsername(anyString())).thenReturn(Optional.empty());

        SeedPatientUser config = new SeedPatientUser();
        CommandLineRunner runner = config.seedPatients(repo, encoder);

        runner.run(new String[0]);

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(repo, times(2)).save(captor.capture());

        var saved = captor.getAllValues();
        assertThat(saved).hasSize(2);

        assertThat(saved.get(0).getUsername()).isEqualTo("testPatient1");
        assertThat(saved.get(0).getFirstName()).isEqualTo("Test");
        assertThat(saved.get(0).getLastName()).isEqualTo("Patient One");

        String hash1 = saved.get(0).getPasswordHash();
        assertThat(hash1).isNotBlank();
        assertThat(hash1).doesNotContain("testPatient1");
        assertThat(encoder.matches("testPatient1", hash1)).isTrue();

        assertThat(saved.get(1).getUsername()).isEqualTo("testPatient2");
        assertThat(saved.get(1).getFirstName()).isEqualTo("Test");
        assertThat(saved.get(1).getLastName()).isEqualTo("Patient Two");
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
}
