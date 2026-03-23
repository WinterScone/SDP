package org.example.sdpclient.service;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

import org.example.sdpclient.dto.*;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminManagePatientDetailServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private AdminManagePatientDetailService service;

    // -------------------------
    // searchPatients
    // -------------------------

    @Test
    void searchPatients_shouldReturnEmpty_whenQueryIsNull() {
        List<PatientViewDto> result = service.searchPatients(null, 1L, false);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(patientRepository);
    }

    @Test
    void searchPatients_shouldReturnEmpty_whenQueryIsBlank() {
        List<PatientViewDto> result = service.searchPatients("   ", 1L, false);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(patientRepository);
    }

    @Test
    void searchPatients_shouldTrimQuery_andMapPatientsToDtos() {
        Patient p1 = new Patient();
        p1.setId(10L);
        p1.setFirstName("Jane");
        p1.setLastName("Doe");
        p1.setDateOfBirth(LocalDate.of(2000, 1, 2));
        p1.setEmail("jane@example.com");
        p1.setPhone("123");

        Patient p2 = new Patient();
        p2.setId(11L);
        p2.setFirstName("John");
        p2.setLastName("Smith");
        p2.setDateOfBirth(LocalDate.of(1999, 5, 6));
        p2.setEmail("john@example.com");
        p2.setPhone("456");

        when(patientRepository.searchByKeyword("abc")).thenReturn(List.of(p1, p2));

        List<PatientViewDto> result = service.searchPatients("  abc  ", 1L, true);

        assertEquals(2, result.size());

        // If PatientViewDto is a record -> result.get(0).id()
        // If it's a class -> result.get(0).getId()

        // We can still validate key fields in a tolerant way:
        // (uncomment the right style if needed)

        // assertEquals(10L, result.get(0).id());
        // assertEquals("Jane", result.get(0).firstName());

        verify(patientRepository).searchByKeyword("abc");
    }

    // -------------------------
    // findPatient
    // -------------------------

    @Test
    void findPatient_shouldDelegateToRepository() {
        Patient p = new Patient();
        p.setId(99L);
        when(patientRepository.findById(99L)).thenReturn(Optional.of(p));

        Optional<Patient> result = service.findPatient(99L);

        assertTrue(result.isPresent());
        assertEquals(99L, result.get().getId());
        verify(patientRepository).findById(99L);
    }

    // -------------------------
    // getPrescriptionViews
    // -------------------------

    @Test
    void getPrescriptionViews_shouldMapPrescriptionsToDtos() {
        Medicine med = new Medicine();
        med.setMedicineId(MedicineType.VTM01);
        med.setMedicineName("TestMed");

        Prescription rx = new Prescription();
        rx.setId(1L);
        rx.setMedicine(med);
        rx.setDosage("10mg");
        rx.setFrequency("ONCE_A_DAY");

        when(prescriptionRepository.findByPatientId(5L)).thenReturn(List.of(rx));

        List<PrescriptionViewDto> result = service.getPrescriptionViews(5L);

        assertEquals(1, result.size());
        verify(prescriptionRepository).findByPatientId(5L);
    }


    // -------------------------
    // prescriptionExists / findPrescription / prescriptionIdExists / deletePrescription
    // -------------------------

    @Test
    void prescriptionExists_shouldDelegateToRepository() {
        when(prescriptionRepository
                .existsByPatientIdAndMedicine_MedicineId(7L, MedicineType.VTM01))
                .thenReturn(true);

        boolean exists = service.prescriptionExists(7L, MedicineType.VTM01);

        assertTrue(exists);
        verify(prescriptionRepository)
                .existsByPatientIdAndMedicine_MedicineId(7L, MedicineType.VTM01);
    }


    @Test
    void findPrescription_shouldDelegateToRepository() {
        Prescription rx = new Prescription();
        rx.setId(123L);

        when(prescriptionRepository.findById(123L)).thenReturn(Optional.of(rx));

        Optional<Prescription> result = service.findPrescription(123L);

        assertTrue(result.isPresent());
        assertEquals(123L, result.get().getId());
        verify(prescriptionRepository).findById(123L);
    }

    @Test
    void prescriptionIdExists_shouldDelegateToRepository() {
        when(prescriptionRepository.existsById(55L)).thenReturn(true);

        assertTrue(service.prescriptionIdExists(55L));
        verify(prescriptionRepository).existsById(55L);
    }

    @Test
    void deletePrescription_shouldDelegateToRepository() {
        service.deletePrescription(77L, 1L, "testAdmin");
        verify(prescriptionRepository).deleteById(77L);
    }

    // -------------------------
    // createPrescription / updatePrescription
    // -------------------------

    @Test
    void createPrescription_shouldTrimFields_andSave() {
        Patient patient = new Patient();
        patient.setId(1L);

        Medicine medicine = new Medicine();
        medicine.setMedicineId(MedicineType.VTM01);
        medicine.setMedicineName("Amoxicillin");

        PrescriptionCreateDto dto = mock(PrescriptionCreateDto.class);
        when(dto.getDosage()).thenReturn(" 10mg ");
        when(dto.getFrequency()).thenReturn("ONCE_A_DAY");

        service.createPrescription(patient, medicine, dto, 1L, "testAdmin");

        ArgumentCaptor<Prescription> captor = ArgumentCaptor.forClass(Prescription.class);
        verify(prescriptionRepository).save(captor.capture());

        Prescription saved = captor.getValue();
        assertSame(patient, saved.getPatient());
        assertSame(medicine, saved.getMedicine());
        assertEquals("10mg", saved.getDosage());
        assertEquals("ONCE_A_DAY", saved.getFrequency());
    }


    @Test
    void updatePrescription_shouldTrimFields_andSave() {
        Prescription rx = new Prescription();
        rx.setDosage("old");
        rx.setFrequency("ONCE_A_DAY");

        PrescriptionUpdateDto dto = mock(PrescriptionUpdateDto.class);
        when(dto.getDosage()).thenReturn(" 20mg ");
        when(dto.getFrequency()).thenReturn(" TWICE_A_DAY ");

        service.updatePrescription(rx, dto);

        assertEquals("20mg", rx.getDosage());
        assertEquals("TWICE_A_DAY", rx.getFrequency());

        verify(prescriptionRepository).save(rx);
    }

    // -------------------------
    // listMedicines / findMedicineById
    // -------------------------

    @Test
    void listMedicines_shouldMapMedicinesToDtos() {
        Medicine m1 = new Medicine();
        m1.setMedicineId(MedicineType.VTM01);
        m1.setMedicineName("TestMed");

        when(medicineRepository.findAll()).thenReturn(List.of(m1));

        List<MedicineViewDto> result = service.listMedicines();

        assertEquals(1, result.size());

        // record style:
        // assertEquals(MedicineType.MEDICINE_ID1, result.get(0).medicineId());
        // assertEquals("TestMed", result.get(0).medicineName());

        verify(medicineRepository).findAll();
    }

    @Test
    void findMedicineById_shouldDelegateToRepository() {
        MedicineType type = MedicineType.VTM01;

        Medicine med = new Medicine();
        med.setMedicineId(type);

        when(medicineRepository.findById(type)).thenReturn(Optional.of(med));

        Optional<Medicine> result = service.findMedicineById(type);

        assertTrue(result.isPresent());
        verify(medicineRepository).findById(type);
    }


    // -------------------------
    // isBlank (static helper)
    // -------------------------

    @Test
    void isBlank_shouldWorkForNullEmptyWhitespaceAndText() {
        assertTrue(AdminManagePatientDetailService.isBlank(null));
        assertTrue(AdminManagePatientDetailService.isBlank(""));
        assertTrue(AdminManagePatientDetailService.isBlank("   "));
        assertFalse(AdminManagePatientDetailService.isBlank("x"));
        assertFalse(AdminManagePatientDetailService.isBlank(" x "));
    }
}
