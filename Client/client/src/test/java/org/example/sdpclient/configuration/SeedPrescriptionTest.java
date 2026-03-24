package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.enums.FrequencyType;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeedPrescriptionTest {

    @Test
    void seed_doesNothing_whenPatientNotFound() throws Exception {
        PatientRepository patientRepo = mock(PatientRepository.class);
        MedicineRepository medicineRepo = mock(MedicineRepository.class);
        PrescriptionRepository prescriptionRepo = mock(PrescriptionRepository.class);

        when(patientRepo.findByUsername("testPatient1")).thenReturn(Optional.empty());

        var runner = new SeedPrescription().seed(patientRepo, medicineRepo, prescriptionRepo);

        runner.run();

        verify(patientRepo).findByUsername("testPatient1");
        verifyNoInteractions(medicineRepo);
        verifyNoInteractions(prescriptionRepo);
    }

    @Test
    void seed_skips_whenPrescriptionAlreadyExists() throws Exception {
        PatientRepository patientRepo = mock(PatientRepository.class);
        MedicineRepository medicineRepo = mock(MedicineRepository.class);
        PrescriptionRepository prescriptionRepo = mock(PrescriptionRepository.class);

        Patient patient = new Patient();
        patient.setId(1L);

        when(patientRepo.findByUsername("testPatient1")).thenReturn(Optional.of(patient));

        when(prescriptionRepo.existsByPatientIdAndMedicine_MedicineId(1L, MedicineType.VTM01.getId())).thenReturn(true);
        when(prescriptionRepo.existsByPatientIdAndMedicine_MedicineId(1L, MedicineType.VTM02.getId())).thenReturn(true);
        when(prescriptionRepo.existsByPatientIdAndMedicine_MedicineId(1L, MedicineType.VTM03.getId())).thenReturn(true);

        var runner = new SeedPrescription().seed(patientRepo, medicineRepo, prescriptionRepo);

        runner.run();

        verify(prescriptionRepo, times(3))
                .existsByPatientIdAndMedicine_MedicineId(eq(1L), any());

        verifyNoInteractions(medicineRepo);
        verify(prescriptionRepo, never()).save(any(Prescription.class));
    }

    @Test
    void seed_createsMissingPrescriptions_whenPatientAndMedicineExist() throws Exception {
        PatientRepository patientRepo = mock(PatientRepository.class);
        MedicineRepository medicineRepo = mock(MedicineRepository.class);
        PrescriptionRepository prescriptionRepo = mock(PrescriptionRepository.class);

        Patient patient = new Patient();
        patient.setId(1L);

        when(patientRepo.findByUsername("testPatient1")).thenReturn(Optional.of(patient));

        when(prescriptionRepo.existsByPatientIdAndMedicine_MedicineId(eq(1L), any())).thenReturn(false);

        Medicine med1 = new Medicine();
        med1.setMedicineId(MedicineType.VTM01.getId());

        Medicine med2 = new Medicine();
        med2.setMedicineId(MedicineType.VTM02.getId());

        Medicine med3 = new Medicine();
        med3.setMedicineId(MedicineType.VTM03.getId());

        when(medicineRepo.findById(MedicineType.VTM01.getId())).thenReturn(Optional.of(med1));
        when(medicineRepo.findById(MedicineType.VTM02.getId())).thenReturn(Optional.of(med2));
        when(medicineRepo.findById(MedicineType.VTM03.getId())).thenReturn(Optional.of(med3));

        ArgumentCaptor<Prescription> captor = ArgumentCaptor.forClass(Prescription.class);

        var runner = new SeedPrescription().seed(patientRepo, medicineRepo, prescriptionRepo);
        runner.run();

        verify(prescriptionRepo, times(3)).save(captor.capture());

        var saved = captor.getAllValues();
        assertEquals(3, saved.size());

        Prescription p1 = saved.get(0);
        assertSame(patient, p1.getPatient());
        assertSame(med1, p1.getMedicine());
        assertEquals("1000", p1.getDosage());
        assertEquals("TWICE_A_DAY", p1.getFrequency());

        Prescription p2 = saved.get(1);
        assertSame(med2, p2.getMedicine());
        assertEquals("268", p2.getDosage());
        assertEquals("ONCE_A_DAY", p2.getFrequency());

        Prescription p3 = saved.get(2);
        assertSame(med3, p3.getMedicine());
        assertEquals("100", p3.getDosage());
        assertEquals("FOUR_TIMES_A_DAY", p3.getFrequency());
    }

    @Test
    void seed_skipsSaving_whenMedicineNotFound() throws Exception {
        PatientRepository patientRepo = mock(PatientRepository.class);
        MedicineRepository medicineRepo = mock(MedicineRepository.class);
        PrescriptionRepository prescriptionRepo = mock(PrescriptionRepository.class);

        Patient patient = new Patient();
        patient.setId(1L);

        when(patientRepo.findByUsername("testPatient1")).thenReturn(Optional.of(patient));
        when(prescriptionRepo.existsByPatientIdAndMedicine_MedicineId(eq(1L), any())).thenReturn(false);

        when(medicineRepo.findById(any())).thenReturn(Optional.empty());

        var runner = new SeedPrescription().seed(patientRepo, medicineRepo, prescriptionRepo);
        runner.run();

        verify(prescriptionRepo, never()).save(any());
    }
}
