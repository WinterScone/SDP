package org.example.sdpclient.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.example.sdpclient.dto.IntakeLogRequest;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.entity.ReminderLog;
import org.example.sdpclient.entity.ReminderStatus;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.example.sdpclient.repository.ReminderLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntakeHistoryServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/London");

    @Mock
    private ReminderLogRepository reminderLogRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PatientRepository patientRepo;

    @Mock
    private ActivityLogService activityLogService;

    private IntakeHistoryService createService(int hour, int minute) {
        ZonedDateTime fixedTime = ZonedDateTime.of(2026, 3, 24, hour, minute, 0, 0, ZONE);
        Clock fixedClock = Clock.fixed(fixedTime.toInstant(), ZONE);
        return new IntakeHistoryService(
                reminderLogRepository, prescriptionRepository, patientRepo,
                activityLogService, fixedClock);
    }

    private Patient createPatient(Long id) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setFirstName("John");
        patient.setLastName("Doe");
        return patient;
    }

    private Prescription createPrescription(Long id, Patient patient, Integer medicineId, String medicineName) {
        Medicine med = new Medicine();
        med.setMedicineId(medicineId);
        med.setMedicineName(medicineName);

        Prescription rx = new Prescription();
        rx.setId(id);
        rx.setPatient(patient);
        rx.setMedicine(med);
        rx.setActive(true);
        return rx;
    }

    @Test
    void logIntake_withMatchingNotifiedEntry_promotesToCollected() {
        IntakeHistoryService service = createService(10, 30);
        Patient patient = createPatient(1L);
        Prescription rx = createPrescription(1L, patient, 101, "Amoxicillin");

        when(patientRepo.findById(1L)).thenReturn(Optional.of(patient));
        when(prescriptionRepository.findByPatientIdAndActiveTrue(1L)).thenReturn(List.of(rx));

        ReminderLog notified = new ReminderLog();
        notified.setId(10L);
        notified.setStatus(ReminderStatus.NOTIFIED);
        notified.setPatient(patient);
        notified.setPrescription(rx);
        notified.setScheduledTime(LocalDateTime.of(2026, 3, 24, 10, 0));

        when(reminderLogRepository
                .findFirstByPatientIdAndPrescription_Medicine_MedicineIdAndStatusAndScheduledTimeBetweenOrderByScheduledTimeAsc(
                        eq(1L), eq(101), eq(ReminderStatus.NOTIFIED), any(), any()))
                .thenReturn(Optional.of(notified));

        IntakeLogRequest req = new IntakeLogRequest();
        req.setMedicineId("101");

        Map<String, Object> result = service.logIntake(1L, req);

        assertTrue((Boolean) result.get("ok"));
        assertEquals(ReminderStatus.COLLECTED, notified.getStatus());
        assertNotNull(notified.getDispensedAt());
        verify(reminderLogRepository).save(notified);
        verify(activityLogService).logMedicineIntakeLogged(eq(1L), eq("John Doe"), eq("Amoxicillin"));
    }

    @Test
    void logIntake_withNoNotifiedEntry_createsNewCollectedEntry() {
        IntakeHistoryService service = createService(10, 30);
        Patient patient = createPatient(1L);
        Prescription rx = createPrescription(1L, patient, 101, "Amoxicillin");

        when(patientRepo.findById(1L)).thenReturn(Optional.of(patient));
        when(prescriptionRepository.findByPatientIdAndActiveTrue(1L)).thenReturn(List.of(rx));
        when(reminderLogRepository
                .findFirstByPatientIdAndPrescription_Medicine_MedicineIdAndStatusAndScheduledTimeBetweenOrderByScheduledTimeAsc(
                        eq(1L), eq(101), eq(ReminderStatus.NOTIFIED), any(), any()))
                .thenReturn(Optional.empty());

        IntakeLogRequest req = new IntakeLogRequest();
        req.setMedicineId("101");

        Map<String, Object> result = service.logIntake(1L, req);

        assertTrue((Boolean) result.get("ok"));

        ArgumentCaptor<ReminderLog> captor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(captor.capture());
        ReminderLog saved = captor.getValue();

        assertEquals(ReminderStatus.COLLECTED, saved.getStatus());
        assertEquals(patient, saved.getPatient());
        assertEquals(rx, saved.getPrescription());
        assertNotNull(saved.getDispensedAt());
    }

    @Test
    void logIntake_patientNotFound_returnsError() {
        IntakeHistoryService service = createService(10, 30);
        when(patientRepo.findById(1L)).thenReturn(Optional.empty());

        IntakeLogRequest req = new IntakeLogRequest();
        req.setMedicineId("101");

        Map<String, Object> result = service.logIntake(1L, req);

        assertFalse((Boolean) result.get("ok"));
        assertEquals("Patient not found", result.get("error"));
    }

    @Test
    void logIntake_invalidMedicineId_returnsError() {
        IntakeHistoryService service = createService(10, 30);
        Patient patient = createPatient(1L);
        when(patientRepo.findById(1L)).thenReturn(Optional.of(patient));

        IntakeLogRequest req = new IntakeLogRequest();
        req.setMedicineId("not-a-number");

        Map<String, Object> result = service.logIntake(1L, req);

        assertFalse((Boolean) result.get("ok"));
        assertEquals("Invalid medicine ID", result.get("error"));
    }

    @Test
    void logIntake_noActivePrescription_returnsError() {
        IntakeHistoryService service = createService(10, 30);
        Patient patient = createPatient(1L);
        when(patientRepo.findById(1L)).thenReturn(Optional.of(patient));
        when(prescriptionRepository.findByPatientIdAndActiveTrue(1L)).thenReturn(List.of());

        IntakeLogRequest req = new IntakeLogRequest();
        req.setMedicineId("101");

        Map<String, Object> result = service.logIntake(1L, req);

        assertFalse((Boolean) result.get("ok"));
        assertEquals("No active prescription found for this medicine", result.get("error"));
    }

    @Test
    void getHistory_returnsTerminalReminderLogEntries() {
        IntakeHistoryService service = createService(10, 30);

        Patient patient = createPatient(1L);
        Prescription rx = createPrescription(1L, patient, 101, "Amoxicillin");

        ReminderLog takenLog = new ReminderLog();
        takenLog.setId(1L);
        takenLog.setPatient(patient);
        takenLog.setPrescription(rx);
        takenLog.setScheduledTime(LocalDateTime.of(2026, 3, 24, 10, 0));
        takenLog.setStatus(ReminderStatus.COLLECTED);

        ReminderLog missedLog = new ReminderLog();
        missedLog.setId(2L);
        missedLog.setPatient(patient);
        missedLog.setPrescription(rx);
        missedLog.setScheduledTime(LocalDateTime.of(2026, 3, 24, 8, 0));
        missedLog.setStatus(ReminderStatus.MISSED);
        missedLog.setFailureReason("Patient unavailable");

        when(reminderLogRepository.findByPatientIdAndStatusInOrderByScheduledTimeDesc(eq(1L), anyList()))
                .thenReturn(List.of(takenLog, missedLog));

        List<Map<String, Object>> history = service.getHistory(1L);

        assertEquals(2, history.size());

        Map<String, Object> first = history.get(0);
        assertEquals(1L, first.get("id"));
        assertEquals(101, first.get("medicineId"));
        assertEquals("Amoxicillin", first.get("medicineName"));
        assertEquals("2026-03-24", first.get("takenDate"));
        assertEquals("10:00", first.get("takenTime"));
        assertEquals("COLLECTED", first.get("status"));
        assertEquals("", first.get("notes"));

        Map<String, Object> second = history.get(1);
        assertEquals("MISSED", second.get("status"));
        assertEquals("Patient unavailable", second.get("notes"));
    }
}
