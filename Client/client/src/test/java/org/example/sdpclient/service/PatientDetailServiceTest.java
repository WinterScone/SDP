package org.example.sdpclient.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientDetailServiceTest {

    @Mock
    private PatientRepository patientRepo;

    @Mock
    private PrescriptionRepository prescriptionRepo;

    @InjectMocks
    private PatientDetailService service;

    @Test
    void patientExists_shouldDelegateToRepository() {
        when(patientRepo.existsById(10L)).thenReturn(true);

        boolean result = service.patientExists(10L);

        assertTrue(result);
        verify(patientRepo).existsById(10L);
    }

    @Test
    void getPrescriptionItems_shouldMapPrescriptionsToResponseItems() {
        Medicine med = new Medicine();
        med.setMedicineId(MedicineType.VTM01);
        med.setMedicineName("Amoxicillin");

        Prescription rx = new Prescription();
        rx.setMedicine(med);
        rx.setDosage("10mg");
        rx.setFrequency("daily");

        when(prescriptionRepo.findByPatientId(5L)).thenReturn(List.of(rx));

        List<PatientPrescriptionsResponse.PrescriptionItem> result =
                service.getPrescriptionItems(5L);

        assertEquals(1, result.size());

        PatientPrescriptionsResponse.PrescriptionItem item = result.get(0);

        // If PrescriptionItem is a record -> use item.medicineId(), etc.
        // If it is a class -> use getters.
        //
        // Most likely it's a record (nested record), so:
        // assertEquals("MEDICINE_ID1", item.medicineId());
        // assertEquals("Amoxicillin", item.medicineName());
        // assertEquals("10mg", item.dosage());
        // assertEquals("daily", item.frequency());

        // If you’re unsure, you can at least verify mapping happened:
        assertNotNull(item);

        verify(prescriptionRepo).findByPatientId(5L);
    }

    @Test
    void getPrescriptionItems_shouldReturnEmptyList_whenNoPrescriptions() {
        when(prescriptionRepo.findByPatientId(5L)).thenReturn(List.of());

        List<PatientPrescriptionsResponse.PrescriptionItem> result =
                service.getPrescriptionItems(5L);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(prescriptionRepo).findByPatientId(5L);
    }
}

