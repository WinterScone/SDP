package org.example.sdpclient.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.example.sdpclient.entity.Admin;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.entity.PrescriptionReminderTime;
import org.example.sdpclient.entity.ReminderLog;
import org.example.sdpclient.entity.ReminderStatus;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.example.sdpclient.repository.ReminderLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoseTrackingServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/London");
    private static final int WINDOW_MINUTES = 15;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private ReminderLogRepository reminderLogRepository;

    @Mock
    private NotificationService notificationService;

    private DoseTrackingService createService(int hour, int minute) {
        ZonedDateTime fixedTime = ZonedDateTime.of(2026, 3, 24, hour, minute, 0, 0, ZONE);
        Clock fixedClock = Clock.fixed(fixedTime.toInstant(), ZONE);
        return new DoseTrackingService(
                prescriptionRepository, reminderLogRepository, notificationService,
                fixedClock, WINDOW_MINUTES);
    }

    private Patient createPatient(Long id, String firstName, String lastName) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        return patient;
    }

    private Prescription createPrescription(Long id, Patient patient, String medicineName,
                                             String dosage, LocalTime... reminderTimes) {
        Medicine med = new Medicine();
        med.setMedicineId(id.intValue());
        med.setMedicineName(medicineName);

        Prescription rx = new Prescription();
        rx.setId(id);
        rx.setPatient(patient);
        rx.setMedicine(med);
        rx.setDosage(dosage);
        rx.setActive(true);

        List<PrescriptionReminderTime> rtList = new ArrayList<>();
        for (LocalTime time : reminderTimes) {
            PrescriptionReminderTime rt = new PrescriptionReminderTime();
            rt.setReminderTime(time);
            rt.setPrescription(rx);
            rtList.add(rt);
        }
        rx.setReminderTimes(rtList);

        return rx;
    }

    // 1. Dose enters window -> NOTIFIED created + SMS sent
    @Test
    void trackDoses_doseEntersWindow_createsNotifiedAndSendsSms() {
        // Dose at 10:00, current time is 09:50 (within 15-min window before 10:00)
        DoseTrackingService service = createService(9, 50);

        Patient patient = createPatient(1L, "John", "Doe");
        Prescription rx = createPrescription(1L, patient, "Amoxicillin", "500mg",
                LocalTime.of(10, 0));

        when(prescriptionRepository.findByActiveTrue()).thenReturn(List.of(rx));
        when(reminderLogRepository.existsByPatientIdAndPrescriptionIdAndScheduledTime(
                eq(1L), eq(1L), any())).thenReturn(false);
        when(reminderLogRepository.findByStatusAndScheduledTimeBefore(
                eq(ReminderStatus.NOTIFIED), any())).thenReturn(List.of());

        service.trackDoses();

        ArgumentCaptor<ReminderLog> captor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(captor.capture());
        ReminderLog saved = captor.getValue();

        assertEquals(ReminderStatus.NOTIFIED, saved.getStatus());
        assertEquals(patient, saved.getPatient());
        assertEquals(rx, saved.getPrescription());
        assertEquals(LocalDateTime.of(2026, 3, 24, 10, 0), saved.getScheduledTime());

        verify(notificationService).notifyPatient(eq(1L), contains("Amoxicillin (500mg)"));
    }

    // 2. Dose already NOTIFIED in window -> no duplicate
    @Test
    void trackDoses_alreadyNotified_noDuplicate() {
        DoseTrackingService service = createService(9, 50);

        Patient patient = createPatient(1L, "John", "Doe");
        Prescription rx = createPrescription(1L, patient, "Amoxicillin", "500mg",
                LocalTime.of(10, 0));

        when(prescriptionRepository.findByActiveTrue()).thenReturn(List.of(rx));
        when(reminderLogRepository.existsByPatientIdAndPrescriptionIdAndScheduledTime(
                eq(1L), eq(1L), any())).thenReturn(true);
        when(reminderLogRepository.findByStatusAndScheduledTimeBefore(
                eq(ReminderStatus.NOTIFIED), any())).thenReturn(List.of());

        service.trackDoses();

        verify(reminderLogRepository, never()).save(any());
        verify(notificationService, never()).notifyPatient(anyLong(), anyString());
    }

    // 3. Dose outside window -> ignored
    @Test
    void trackDoses_doseOutsideWindow_ignored() {
        // Dose at 10:00, current time is 08:00 (way outside window)
        DoseTrackingService service = createService(8, 0);

        Patient patient = createPatient(1L, "John", "Doe");
        Prescription rx = createPrescription(1L, patient, "Amoxicillin", "500mg",
                LocalTime.of(10, 0));

        when(prescriptionRepository.findByActiveTrue()).thenReturn(List.of(rx));
        when(reminderLogRepository.findByStatusAndScheduledTimeBefore(
                eq(ReminderStatus.NOTIFIED), any())).thenReturn(List.of());

        service.trackDoses();

        verify(reminderLogRepository, never()).save(any());
        verify(notificationService, never()).notifyPatient(anyLong(), anyString());
    }

    // 4. Multiple doses same patient -> grouped SMS
    @Test
    void trackDoses_multipleDosesSamePatient_groupedSms() {
        DoseTrackingService service = createService(9, 50);

        Patient patient = createPatient(1L, "John", "Doe");
        Prescription rx1 = createPrescription(1L, patient, "Amoxicillin", "500mg",
                LocalTime.of(10, 0));
        Prescription rx2 = createPrescription(2L, patient, "Ibuprofen", "200mg",
                LocalTime.of(10, 0));

        when(prescriptionRepository.findByActiveTrue()).thenReturn(List.of(rx1, rx2));
        when(reminderLogRepository.existsByPatientIdAndPrescriptionIdAndScheduledTime(
                anyLong(), anyLong(), any())).thenReturn(false);
        when(reminderLogRepository.findByStatusAndScheduledTimeBefore(
                eq(ReminderStatus.NOTIFIED), any())).thenReturn(List.of());

        service.trackDoses();

        verify(reminderLogRepository, times(2)).save(any(ReminderLog.class));
        // Only one SMS sent (grouped)
        verify(notificationService, times(1)).notifyPatient(eq(1L), argThat(msg ->
                msg.contains("Amoxicillin") && msg.contains("Ibuprofen")));
    }

    // 5. Stale NOTIFIED past window -> promoted to MISSED + patient SMS + admin SMS
    @Test
    void trackDoses_staleNotified_promotedToMissedWithAlerts() {
        // Current time 10:20, dose was at 10:00, window is 15 min -> cutoff is 10:05
        DoseTrackingService service = createService(10, 20);

        Admin admin = new Admin();
        admin.setId(99L);

        Patient patient = createPatient(1L, "John", "Doe");
        patient.setLinkedAdmin(admin);

        Medicine med = new Medicine();
        med.setMedicineId(1);
        med.setMedicineName("Amoxicillin");

        Prescription rx = new Prescription();
        rx.setId(1L);
        rx.setPatient(patient);
        rx.setMedicine(med);

        ReminderLog staleLog = new ReminderLog();
        staleLog.setId(10L);
        staleLog.setPatient(patient);
        staleLog.setPrescription(rx);
        staleLog.setScheduledTime(LocalDateTime.of(2026, 3, 24, 10, 0));
        staleLog.setStatus(ReminderStatus.NOTIFIED);

        when(prescriptionRepository.findByActiveTrue()).thenReturn(List.of());
        when(reminderLogRepository.findByStatusAndScheduledTimeBefore(
                eq(ReminderStatus.NOTIFIED), any())).thenReturn(List.of(staleLog));

        service.trackDoses();

        assertEquals(ReminderStatus.MISSED, staleLog.getStatus());
        verify(reminderLogRepository).save(staleLog);
        verify(notificationService).notifyPatient(eq(1L), contains("missed"));
        verify(notificationService).notifyAdmin(eq(99L), contains("missed"));
    }

    // 6. Stale NOTIFIED, patient has no admin -> only patient SMS
    @Test
    void trackDoses_staleNotifiedNoAdmin_onlyPatientSms() {
        DoseTrackingService service = createService(10, 20);

        Patient patient = createPatient(1L, "John", "Doe");
        patient.setLinkedAdmin(null);

        Medicine med = new Medicine();
        med.setMedicineId(1);
        med.setMedicineName("Amoxicillin");

        Prescription rx = new Prescription();
        rx.setId(1L);
        rx.setPatient(patient);
        rx.setMedicine(med);

        ReminderLog staleLog = new ReminderLog();
        staleLog.setId(10L);
        staleLog.setPatient(patient);
        staleLog.setPrescription(rx);
        staleLog.setScheduledTime(LocalDateTime.of(2026, 3, 24, 10, 0));
        staleLog.setStatus(ReminderStatus.NOTIFIED);

        when(prescriptionRepository.findByActiveTrue()).thenReturn(List.of());
        when(reminderLogRepository.findByStatusAndScheduledTimeBefore(
                eq(ReminderStatus.NOTIFIED), any())).thenReturn(List.of(staleLog));

        service.trackDoses();

        assertEquals(ReminderStatus.MISSED, staleLog.getStatus());
        verify(notificationService).notifyPatient(eq(1L), contains("missed"));
        verify(notificationService, never()).notifyAdmin(anyLong(), anyString());
    }

    // 7. COLLECTED entry past window -> no action (already terminal)
    @Test
    void trackDoses_collectedEntryPastWindow_noAction() {
        DoseTrackingService service = createService(10, 20);

        // The stale query only returns NOTIFIED entries, so COLLECTED entries are never touched
        when(prescriptionRepository.findByActiveTrue()).thenReturn(List.of());
        when(reminderLogRepository.findByStatusAndScheduledTimeBefore(
                eq(ReminderStatus.NOTIFIED), any())).thenReturn(List.of());

        service.trackDoses();

        verify(notificationService, never()).notifyPatient(anyLong(), contains("missed"));
        verify(notificationService, never()).notifyAdmin(anyLong(), anyString());
    }

    // 8. Prescription not valid for today (startDate/endDate) -> skipped
    @Test
    void trackDoses_prescriptionNotValidForToday_skipped() {
        DoseTrackingService service = createService(9, 50);

        Patient patient = createPatient(1L, "John", "Doe");
        Prescription rx = createPrescription(1L, patient, "Amoxicillin", "500mg",
                LocalTime.of(10, 0));
        // Prescription starts tomorrow
        rx.setStartDate(LocalDate.of(2026, 3, 25));

        when(prescriptionRepository.findByActiveTrue()).thenReturn(List.of(rx));
        when(reminderLogRepository.findByStatusAndScheduledTimeBefore(
                eq(ReminderStatus.NOTIFIED), any())).thenReturn(List.of());

        service.trackDoses();

        verify(reminderLogRepository, never()).save(any());
        verify(notificationService, never()).notifyPatient(anyLong(), anyString());
    }

    // 9. No active prescriptions -> no action
    @Test
    void trackDoses_noActivePrescriptions_noAction() {
        DoseTrackingService service = createService(10, 0);

        when(prescriptionRepository.findByActiveTrue()).thenReturn(List.of());
        when(reminderLogRepository.findByStatusAndScheduledTimeBefore(
                eq(ReminderStatus.NOTIFIED), any())).thenReturn(List.of());

        service.trackDoses();

        verify(reminderLogRepository, never()).save(any());
        verify(notificationService, never()).notifyPatient(anyLong(), anyString());
        verify(notificationService, never()).notifyAdmin(anyLong(), anyString());
    }

    // Window boundary tests
    @Test
    void isInWindow_exactlyAtNotifyTime_returnsTrue() {
        // reminderTime=10:00, window=15min, so notifyAt=09:45
        // current time = 09:45 -> should be in window
        DoseTrackingService service = createService(9, 45);
        assertTrue(service.isInWindow(LocalTime.of(10, 0), LocalTime.of(9, 45)));
    }

    @Test
    void isInWindow_exactlyAtReminderTime_returnsTrue() {
        DoseTrackingService service = createService(10, 0);
        assertTrue(service.isInWindow(LocalTime.of(10, 0), LocalTime.of(10, 0)));
    }

    @Test
    void isInWindow_beforeWindow_returnsFalse() {
        DoseTrackingService service = createService(9, 44);
        assertFalse(service.isInWindow(LocalTime.of(10, 0), LocalTime.of(9, 44)));
    }

    @Test
    void isInWindow_afterReminderTime_returnsFalse() {
        DoseTrackingService service = createService(10, 1);
        assertFalse(service.isInWindow(LocalTime.of(10, 0), LocalTime.of(10, 1)));
    }

    @Test
    void isInWindow_midnightWrap_returnsFalse() {
        // reminderTime=00:05, window=15min -> notifyAt=23:50 (wraps past midnight)
        DoseTrackingService service = createService(23, 50);
        assertFalse(service.isInWindow(LocalTime.of(0, 5), LocalTime.of(23, 50)));
    }

    // -------------------------
    // checkAndNotifyPatient tests
    // -------------------------

    @Test
    void checkAndNotifyPatient_doseInFullWindow_createsNotifiedAndNotifies() {
        // Prescription with reminder at 10:00, current time 10:05 (past reminder but within collection window)
        DoseTrackingService service = createService(10, 5);

        Patient patient = createPatient(1L, "John", "Doe");
        Prescription rx = createPrescription(1L, patient, "Amoxicillin", "500mg",
                LocalTime.of(10, 0));

        when(prescriptionRepository.findByPatientIdAndActiveTrue(1L)).thenReturn(List.of(rx));
        when(reminderLogRepository.existsByPatientIdAndPrescriptionIdAndScheduledTime(
                eq(1L), eq(1L), any())).thenReturn(false);

        service.checkAndNotifyPatient(1L);

        ArgumentCaptor<ReminderLog> captor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(captor.capture());
        ReminderLog saved = captor.getValue();

        assertEquals(ReminderStatus.NOTIFIED, saved.getStatus());
        assertEquals(patient, saved.getPatient());
        assertEquals(rx, saved.getPrescription());
        assertEquals(LocalDateTime.of(2026, 3, 24, 10, 0), saved.getScheduledTime());

        verify(notificationService).notifyPatient(eq(1L), contains("ready for collection"));
        verify(notificationService).notifyPatient(eq(1L), contains("Amoxicillin (500mg)"));
    }

    @Test
    void checkAndNotifyPatient_doseOutsideWindow_noAction() {
        // Reminder at 10:00, current time 10:20 (outside the ±15 min window)
        DoseTrackingService service = createService(10, 20);

        Patient patient = createPatient(1L, "John", "Doe");
        Prescription rx = createPrescription(1L, patient, "Amoxicillin", "500mg",
                LocalTime.of(10, 0));

        when(prescriptionRepository.findByPatientIdAndActiveTrue(1L)).thenReturn(List.of(rx));

        service.checkAndNotifyPatient(1L);

        verify(reminderLogRepository, never()).save(any());
        verify(notificationService, never()).notifyPatient(anyLong(), anyString());
    }

    @Test
    void checkAndNotifyPatient_alreadyNotified_noDuplicate() {
        // Reminder at 10:00, current time 10:05, but entry already exists
        DoseTrackingService service = createService(10, 5);

        Patient patient = createPatient(1L, "John", "Doe");
        Prescription rx = createPrescription(1L, patient, "Amoxicillin", "500mg",
                LocalTime.of(10, 0));

        when(prescriptionRepository.findByPatientIdAndActiveTrue(1L)).thenReturn(List.of(rx));
        when(reminderLogRepository.existsByPatientIdAndPrescriptionIdAndScheduledTime(
                eq(1L), eq(1L), any())).thenReturn(true);

        service.checkAndNotifyPatient(1L);

        verify(reminderLogRepository, never()).save(any());
        verify(notificationService, never()).notifyPatient(anyLong(), anyString());
    }

    // -------------------------
    // isInFullWindow boundary tests
    // -------------------------

    @Test
    void isInFullWindow_beforeWindowStart_returnsFalse() {
        DoseTrackingService service = createService(9, 44);
        assertFalse(service.isInFullWindow(LocalTime.of(10, 0), LocalTime.of(9, 44)));
    }

    @Test
    void isInFullWindow_atWindowStart_returnsTrue() {
        DoseTrackingService service = createService(9, 45);
        assertTrue(service.isInFullWindow(LocalTime.of(10, 0), LocalTime.of(9, 45)));
    }

    @Test
    void isInFullWindow_afterReminderTime_withinWindow_returnsTrue() {
        DoseTrackingService service = createService(10, 10);
        assertTrue(service.isInFullWindow(LocalTime.of(10, 0), LocalTime.of(10, 10)));
    }

    @Test
    void isInFullWindow_atWindowEnd_returnsTrue() {
        DoseTrackingService service = createService(10, 15);
        assertTrue(service.isInFullWindow(LocalTime.of(10, 0), LocalTime.of(10, 15)));
    }

    @Test
    void isInFullWindow_afterWindowEnd_returnsFalse() {
        DoseTrackingService service = createService(10, 16);
        assertFalse(service.isInFullWindow(LocalTime.of(10, 0), LocalTime.of(10, 16)));
    }

    @Test
    void isInFullWindow_midnightWrap_returnsFalse() {
        DoseTrackingService service = createService(23, 50);
        assertFalse(service.isInFullWindow(LocalTime.of(0, 5), LocalTime.of(23, 50)));
    }
}
