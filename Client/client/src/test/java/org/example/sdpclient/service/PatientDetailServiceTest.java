package org.example.sdpclient.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.entity.PrescriptionReminderTime;
import org.example.sdpclient.entity.ReminderLog;
import org.example.sdpclient.entity.ReminderStatus;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.DispenserSlotRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.example.sdpclient.repository.ReminderLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientDetailServiceTest {

    @Mock
    private PatientRepository patientRepo;

    @Mock
    private PrescriptionRepository prescriptionRepo;

    @Mock
    private DispenserSlotRepository dispenserSlotRepo;

    @Mock
    private ReminderLogRepository reminderLogRepo;

    private PatientDetailService service;

    // Fixed clock: 2026-03-24 10:00:00 London time
    private static final ZoneId ZONE = ZoneId.of("Europe/London");
    private static final Instant FIXED_INSTANT = LocalDate.of(2026, 3, 24)
            .atTime(10, 0).atZone(ZONE).toInstant();
    private static final Clock CLOCK = Clock.fixed(FIXED_INSTANT, ZONE);
    private static final LocalTime NOW = LocalTime.of(10, 0);
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 24);

    @BeforeEach
    void setUp() {
        service = new PatientDetailService(patientRepo, prescriptionRepo,
                dispenserSlotRepo, reminderLogRepo, CLOCK);
    }

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
        med.setMedicineId(MedicineType.VTM01.getId());
        med.setMedicineName("Amoxicillin");

        Prescription rx = new Prescription();
        rx.setId(1L);
        rx.setMedicine(med);
        rx.setDosage("10mg");
        rx.setFrequency(1);

        when(prescriptionRepo.findByPatientId(5L)).thenReturn(List.of(rx));

        List<PatientPrescriptionsResponse.PrescriptionItem> result =
                service.getPrescriptionItems(5L);

        assertEquals(1, result.size());

        PatientPrescriptionsResponse.PrescriptionItem item = result.get(0);

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

    @Test
    void getCollectableItems_shouldReturnDueItems() {
        LocalTime now = NOW;

        Medicine med = new Medicine();
        med.setMedicineId(MedicineType.VTM01.getId());
        med.setMedicineName("Amoxicillin");

        Prescription rx = new Prescription();
        rx.setId(1L);
        rx.setMedicine(med);
        rx.setDosage("10mg");
        rx.setActive(true);
        rx.setStartDate(TODAY.minusDays(1));
        rx.setEndDate(TODAY.plusDays(1));

        PrescriptionReminderTime rt = new PrescriptionReminderTime();
        rt.setReminderTime(now);
        rt.setPrescription(rx);
        rx.setReminderTimes(List.of(rt));

        when(prescriptionRepo.findByPatientIdAndActiveTrue(5L)).thenReturn(List.of(rx));
        when(reminderLogRepo.findByPatientIdAndPrescriptionIdAndScheduledTime(anyLong(), anyLong(), any()))
                .thenReturn(Optional.empty());
        when(dispenserSlotRepo.findByMedicineAndActiveTrue(med)).thenReturn(Optional.empty());

        List<PatientPrescriptionsResponse.PrescriptionItem> result =
                service.getCollectableItems(5L);

        assertEquals(1, result.size());
        assertEquals("Amoxicillin", result.get(0).getMedicineName());
        assertEquals("10mg", result.get(0).getDosage());
        assertEquals(1L, result.get(0).getPrescriptionId());
    }

    @Test
    void getCollectableItems_shouldExcludeAlreadyDispensedItems() {
        LocalTime now = NOW;

        Medicine med = new Medicine();
        med.setMedicineId(MedicineType.VTM01.getId());
        med.setMedicineName("Amoxicillin");

        Prescription rx = new Prescription();
        rx.setId(1L);
        rx.setMedicine(med);
        rx.setDosage("10mg");
        rx.setActive(true);

        PrescriptionReminderTime rt = new PrescriptionReminderTime();
        rt.setReminderTime(now);
        rt.setPrescription(rx);
        rx.setReminderTimes(List.of(rt));

        ReminderLog log = new ReminderLog();
        log.setStatus(ReminderStatus.COLLECTED);

        when(prescriptionRepo.findByPatientIdAndActiveTrue(5L)).thenReturn(List.of(rx));
        when(reminderLogRepo.findByPatientIdAndPrescriptionIdAndScheduledTime(anyLong(), anyLong(), any()))
                .thenReturn(Optional.of(log));

        List<PatientPrescriptionsResponse.PrescriptionItem> result =
                service.getCollectableItems(5L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCollectableItems_shouldIncludeNotifiedItems() {
        LocalTime now = NOW;

        Medicine med = new Medicine();
        med.setMedicineId(MedicineType.VTM01.getId());
        med.setMedicineName("Amoxicillin");

        Prescription rx = new Prescription();
        rx.setId(1L);
        rx.setMedicine(med);
        rx.setDosage("10mg");
        rx.setActive(true);

        PrescriptionReminderTime rt = new PrescriptionReminderTime();
        rt.setReminderTime(now);
        rt.setPrescription(rx);
        rx.setReminderTimes(List.of(rt));

        ReminderLog log = new ReminderLog();
        log.setStatus(ReminderStatus.NOTIFIED);

        when(prescriptionRepo.findByPatientIdAndActiveTrue(5L)).thenReturn(List.of(rx));
        when(reminderLogRepo.findByPatientIdAndPrescriptionIdAndScheduledTime(anyLong(), anyLong(), any()))
                .thenReturn(Optional.of(log));
        when(dispenserSlotRepo.findByMedicineAndActiveTrue(med)).thenReturn(Optional.empty());

        List<PatientPrescriptionsResponse.PrescriptionItem> result =
                service.getCollectableItems(5L);

        assertEquals(1, result.size());
        assertEquals("Amoxicillin", result.get(0).getMedicineName());
    }

    @Test
    void getCollectableItems_shouldExcludeItemsOutsideTimeWindow() {
        Medicine med = new Medicine();
        med.setMedicineId(MedicineType.VTM01.getId());
        med.setMedicineName("Amoxicillin");

        Prescription rx = new Prescription();
        rx.setId(1L);
        rx.setMedicine(med);
        rx.setDosage("10mg");
        rx.setActive(true);

        PrescriptionReminderTime rt = new PrescriptionReminderTime();
        rt.setReminderTime(NOW.plusHours(3));
        rt.setPrescription(rx);
        rx.setReminderTimes(List.of(rt));

        when(prescriptionRepo.findByPatientIdAndActiveTrue(5L)).thenReturn(List.of(rx));

        List<PatientPrescriptionsResponse.PrescriptionItem> result =
                service.getCollectableItems(5L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCollectableItems_shouldReturnEmptyList_whenNoActivePrescriptions() {
        when(prescriptionRepo.findByPatientIdAndActiveTrue(5L)).thenReturn(List.of());

        List<PatientPrescriptionsResponse.PrescriptionItem> result =
                service.getCollectableItems(5L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
