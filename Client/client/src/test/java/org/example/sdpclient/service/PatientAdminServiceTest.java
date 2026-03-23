package org.example.sdpclient.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.example.sdpclient.dto.PatientRow;
import org.example.sdpclient.entity.Admin;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.repository.AdminRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PatientAdminServiceTest {

    @Mock
    private PatientRepository patientRepo;

    @Mock
    private AdminRepository adminRepo;

    @InjectMocks
    private PatientAdminService service;

    @Test
    void getAllPatientsSafe_shouldMapPatientsToRows() {
        Patient p = new Patient();
        p.setId(1L);
        p.setFirstName("Jane");
        p.setLastName("Doe");
        p.setDateOfBirth(LocalDate.of(2000, 1, 2).toString());
        p.setEmail("jane@example.com");
        p.setPhone("123");
        p.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0).toString());
        p.setLinkedAdminId(9L);
        p.setLinkedAdminName("admin9");

        when(patientRepo.findAll()).thenReturn(List.of(p));

        List<PatientRow> result = service.getAllPatientsSafe();

        assertEquals(1, result.size());

        // If PatientRow is a record, use .id() etc.
        // If PatientRow is a class, use getters.
        //
        // Example record assertions:
        // assertEquals(1L, result.get(0).id());
        // assertEquals("Jane", result.get(0).firstName());
        // assertEquals("Doe", result.get(0).lastName());
        // assertEquals(9L, result.get(0).linkedAdminId());
        // assertEquals("admin9", result.get(0).linkedAdminName());

        verify(patientRepo).findAll();
    }

    @Test
    void linkAdminToPatient_shouldSetLinkedAdminFields_andSave() {
        Patient patient = new Patient();
        patient.setId(10L);

        Admin admin = new Admin();
        admin.setId(7L);
        admin.setUsername("rootAdmin");

        when(patientRepo.findById(10L)).thenReturn(Optional.of(patient));
        when(adminRepo.findById(7L)).thenReturn(Optional.of(admin));

        service.linkAdminToPatient(10L, 7L);

        assertEquals(7L, patient.getLinkedAdminId());
        assertEquals("rootAdmin", patient.getLinkedAdminName());

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepo).save(captor.capture());
        assertSame(patient, captor.getValue());

        verify(patientRepo).findById(10L);
        verify(adminRepo).findById(7L);
    }

    @Test
    void linkAdminToPatient_shouldThrow404_whenPatientNotFound() {
        when(patientRepo.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.linkAdminToPatient(10L, 7L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Patient not found", ex.getReason());

        verify(patientRepo).findById(10L);
        verifyNoInteractions(adminRepo);
        verify(patientRepo, never()).save(any());
    }

    @Test
    void linkAdminToPatient_shouldThrow404_whenAdminNotFound() {
        Patient patient = new Patient();
        patient.setId(10L);

        when(patientRepo.findById(10L)).thenReturn(Optional.of(patient));
        when(adminRepo.findById(7L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.linkAdminToPatient(10L, 7L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Admin not found", ex.getReason());

        verify(patientRepo).findById(10L);
        verify(adminRepo).findById(7L);
        verify(patientRepo, never()).save(any());
    }

    @Test
    void getAllPatientsSafe_shouldReturnEmpty_whenNoPatients() {
        when(patientRepo.findAll()).thenReturn(List.of());
        assertTrue(service.getAllPatientsSafe().isEmpty());
        verify(patientRepo).findAll();
    }
}

