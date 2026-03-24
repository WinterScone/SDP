package org.example.sdpclient.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.example.sdpclient.dto.PatientLogin;
import org.example.sdpclient.dto.PatientSignup;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PatientLoginServiceTest {

    @Mock
    private PatientRepository repo;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private PatientLoginService service;

    // -------------------------
    // login()
    // -------------------------

    @Test
    void login_shouldReturnOkFalse_whenUserNotFound() {
        PatientLogin req = mock(PatientLogin.class);
        when(req.getUsername()).thenReturn("missing");

        when(repo.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

        Map<String, Object> result = service.login(req);

        assertEquals(false, result.get("ok"));
        assertEquals(1, result.size());

        verify(repo).findByUsernameIgnoreCase("missing");
        verifyNoInteractions(encoder);
    }

    @Test
    void login_shouldReturnOkFalse_whenPasswordDoesNotMatch() {
        PatientLogin req = mock(PatientLogin.class);
        when(req.getUsername()).thenReturn("user");
        when(req.getPassword()).thenReturn("wrong");

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setUsername("user");
        patient.setPasswordHash("hashed");

        when(repo.findByUsernameIgnoreCase("user")).thenReturn(Optional.of(patient));
        when(encoder.matches("wrong", "hashed")).thenReturn(false);

        Map<String, Object> result = service.login(req);

        assertEquals(false, result.get("ok"));
        assertEquals(1, result.size());

        verify(repo).findByUsernameIgnoreCase("user");
        verify(encoder).matches("wrong", "hashed");
    }

    @Test
    void login_shouldReturnOkTrueWithUsernameAndPatientId_whenPasswordMatches() {
        PatientLogin req = mock(PatientLogin.class);
        when(req.getUsername()).thenReturn("user");
        when(req.getPassword()).thenReturn("pw");

        Patient patient = new Patient();
        patient.setId(42L);
        patient.setUsername("user");
        patient.setPasswordHash("hashed");

        when(repo.findByUsernameIgnoreCase("user")).thenReturn(Optional.of(patient));
        when(encoder.matches("pw", "hashed")).thenReturn(true);

        Map<String, Object> result = service.login(req);

        assertEquals(true, result.get("ok"));
        assertEquals("user", result.get("username"));
        assertEquals(42L, result.get("patientId"));

        verify(repo).findByUsernameIgnoreCase("user");
        verify(encoder).matches("pw", "hashed");
    }

    // -------------------------
    // signup()
    // -------------------------

    @Test
    void signup_shouldFail_whenMissingRequiredFields() {
        PatientSignup req = mock(PatientSignup.class);
        // Trigger the "missing fields" branch with just one missing/blank required field:
        when(req.getUsername()).thenReturn("   "); // blank -> fails immediately

        Map<String, Object> result = service.signup(req);

        assertEquals(false, result.get("ok"));
        assertEquals("Missing required fields", result.get("error"));

        verifyNoInteractions(repo);
        verifyNoInteractions(encoder);
    }

    @Test
    void signup_shouldFail_whenUsernameAlreadyTaken() {
        PatientSignup req = mock(PatientSignup.class);
        when(req.getUsername()).thenReturn(" user ");
        when(req.getPassword()).thenReturn("pw");
        when(req.getFirstName()).thenReturn("John");
        when(req.getLastName()).thenReturn("Doe");
        when(req.getDateOfBirth()).thenReturn("2000-01-02");

        when(repo.existsByUsernameIgnoreCase("user")).thenReturn(true);

        Map<String, Object> result = service.signup(req);

        assertEquals(false, result.get("ok"));
        assertEquals("Username already taken", result.get("error"));

        verify(repo).existsByUsernameIgnoreCase("user");
        verify(repo, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void signup_shouldSavePatient_andSetEmailPhoneNull_whenBlank() {
        PatientSignup req = mock(PatientSignup.class);
        when(req.getUsername()).thenReturn(" user ");
        when(req.getPassword()).thenReturn("pw");
        when(req.getFirstName()).thenReturn(" John ");
        when(req.getLastName()).thenReturn(" Doe ");
        when(req.getDateOfBirth()).thenReturn(" 2000-01-02 ");
        when(req.getEmail()).thenReturn("   "); // blank -> null
        when(req.getPhone()).thenReturn(null);  // null -> null

        when(repo.existsByUsernameIgnoreCase("user")).thenReturn(false);
        when(encoder.encode("pw")).thenReturn("hashedPw");

        // Return a "saved" patient with an id
        Patient saved = new Patient();
        saved.setId(99L);
        saved.setUsername("user");
        when(repo.save(any(Patient.class))).thenReturn(saved);

        Map<String, Object> result = service.signup(req);

        assertEquals(true, result.get("ok"));
        assertEquals(99L, result.get("id"));
        assertEquals("user", result.get("username"));

        // Capture what we saved to verify trimming and null-handling
        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(repo).save(captor.capture());

        Patient toSave = captor.getValue();
        assertEquals("user", toSave.getUsername());
        assertEquals("hashedPw", toSave.getPasswordHash());
        assertEquals("John", toSave.getFirstName());
        assertEquals("Doe", toSave.getLastName());
        assertEquals(LocalDate.parse("2000-01-02"), toSave.getDateOfBirth());
        assertNull(toSave.getEmail());
        assertNull(toSave.getPhone());
        assertNull(toSave.getLinkedAdmin());

        verify(repo).existsByUsernameIgnoreCase("user");
        verify(encoder).encode("pw");
    }

    @Test
    void signup_shouldTrimEmailAndPhone_whenProvided() {
        PatientSignup req = mock(PatientSignup.class);
        when(req.getUsername()).thenReturn("user");
        when(req.getPassword()).thenReturn("pw");
        when(req.getFirstName()).thenReturn("John");
        when(req.getLastName()).thenReturn("Doe");
        when(req.getDateOfBirth()).thenReturn("2000-01-02");
        when(req.getEmail()).thenReturn("  a@b.com  ");
        when(req.getPhone()).thenReturn("  07700 900002  ");

        when(repo.existsByUsernameIgnoreCase("user")).thenReturn(false);
        when(encoder.encode("pw")).thenReturn("hashedPw");

        Patient saved = new Patient();
        saved.setId(1L);
        saved.setUsername("user");
        when(repo.save(any(Patient.class))).thenReturn(saved);

        service.signup(req);

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(repo).save(captor.capture());

        Patient toSave = captor.getValue();
        assertEquals("a@b.com", toSave.getEmail());
        assertEquals("07700900002", toSave.getPhone());
    }
}

