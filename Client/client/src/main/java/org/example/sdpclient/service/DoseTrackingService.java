package org.example.sdpclient.service;

import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.entity.PrescriptionReminderTime;
import org.example.sdpclient.entity.ReminderLog;
import org.example.sdpclient.entity.ReminderStatus;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.example.sdpclient.repository.ReminderLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DoseTrackingService {

    private static final Logger log = LoggerFactory.getLogger(DoseTrackingService.class);

    private final PrescriptionRepository prescriptionRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final NotificationService notificationService;
    private final Clock clock;
    private final int collectionWindowMinutes;

    @Autowired
    public DoseTrackingService(PrescriptionRepository prescriptionRepository,
                               ReminderLogRepository reminderLogRepository,
                               NotificationService notificationService,
                               @Value("${app.timezone:Europe/London}") String timezone,
                               @Value("${app.collection-window-minutes:15}") int collectionWindowMinutes) {
        this(prescriptionRepository, reminderLogRepository, notificationService,
                Clock.system(ZoneId.of(timezone)), collectionWindowMinutes);
    }

    DoseTrackingService(PrescriptionRepository prescriptionRepository,
                        ReminderLogRepository reminderLogRepository,
                        NotificationService notificationService,
                        Clock clock,
                        int collectionWindowMinutes) {
        this.prescriptionRepository = prescriptionRepository;
        this.reminderLogRepository = reminderLogRepository;
        this.notificationService = notificationService;
        this.clock = clock;
        this.collectionWindowMinutes = collectionWindowMinutes;
    }

    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void trackDoses() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        log.info("Running dose tracking at {}", now);

        List<Prescription> activePrescriptions = prescriptionRepository.findByActiveTrue();
        Map<Long, List<String>> patientMedicines = new HashMap<>();
        List<ReminderLog> logsToSave = new ArrayList<>();

        // Phase 1: Check doses entering the window → create NOTIFIED entries
        for (Prescription p : activePrescriptions) {
            if (p.getReminderTimes() == null || p.getReminderTimes().isEmpty()) {
                continue;
            }

            Patient patient = p.getPatient();
            if (patient == null) {
                continue;
            }

            if (!isPrescriptionValidForToday(p, today)) {
                continue;
            }

            for (PrescriptionReminderTime rt : p.getReminderTimes()) {
                LocalTime reminderTime = rt.getReminderTime();

                if (!isInWindow(reminderTime, currentTime)) {
                    continue;
                }

                LocalDateTime scheduledDateTime = LocalDateTime.of(today, reminderTime);

                boolean alreadyLogged = reminderLogRepository
                        .existsByPatientIdAndPrescriptionIdAndScheduledTime(
                                patient.getId(), p.getId(), scheduledDateTime);
                if (alreadyLogged) {
                    continue;
                }

                patientMedicines.computeIfAbsent(patient.getId(), k -> new ArrayList<>())
                        .add(p.getMedicine().getMedicineName() + " (" + p.getDosage() + ")");

                ReminderLog reminderLog = new ReminderLog();
                reminderLog.setPatient(patient);
                reminderLog.setPrescription(p);
                reminderLog.setScheduledTime(scheduledDateTime);
                reminderLog.setStatus(ReminderStatus.NOTIFIED);
                logsToSave.add(reminderLog);
            }
        }

        for (ReminderLog rl : logsToSave) {
            reminderLogRepository.save(rl);
        }

        for (Map.Entry<Long, List<String>> entry : patientMedicines.entrySet()) {
            String medicines = String.join(", ", entry.getValue());
            String message = "SDP Reminder: Your medication collection window opens in 15 minutes — " + medicines;
            notificationService.notifyPatient(entry.getKey(), message);
        }

        log.info("Dose tracking: notified {} patients", patientMedicines.size());

        // Phase 2: Promote stale NOTIFIED entries to MISSED
        LocalDateTime cutoff = now.minusMinutes(collectionWindowMinutes);
        List<ReminderLog> stale = reminderLogRepository
                .findByStatusAndScheduledTimeBefore(ReminderStatus.NOTIFIED, cutoff);

        for (ReminderLog entry : stale) {
            entry.setStatus(ReminderStatus.MISSED);
            reminderLogRepository.save(entry);

            Patient patient = entry.getPatient();
            String medicineName = entry.getPrescription().getMedicine().getMedicineName();
            LocalTime scheduledTime = entry.getScheduledTime().toLocalTime();

            notificationService.notifyPatient(patient.getId(),
                    "SDP Alert: You missed your " + medicineName + " dose scheduled for "
                            + scheduledTime + ". Please take it as soon as possible.");

            if (patient.getLinkedAdmin() != null) {
                notificationService.notifyAdmin(patient.getLinkedAdmin().getId(),
                        "SDP Alert: Patient " + patient.getFirstName() + " " + patient.getLastName()
                                + " missed their " + medicineName + " dose scheduled for "
                                + scheduledTime + ".");
            }
        }

        if (!stale.isEmpty()) {
            log.info("Dose tracking: promoted {} stale entries to MISSED", stale.size());
        }
    }

    @Transactional
    public void checkAndNotifyPatient(Long patientId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdAndActiveTrue(patientId);
        List<String> medicines = new ArrayList<>();
        List<ReminderLog> logsToSave = new ArrayList<>();

        for (Prescription p : prescriptions) {
            if (p.getReminderTimes() == null || p.getReminderTimes().isEmpty()) continue;
            Patient patient = p.getPatient();
            if (patient == null) continue;
            if (!isPrescriptionValidForToday(p, today)) continue;

            for (PrescriptionReminderTime rt : p.getReminderTimes()) {
                LocalTime reminderTime = rt.getReminderTime();
                if (!isInFullWindow(reminderTime, currentTime)) continue;

                LocalDateTime scheduledDateTime = LocalDateTime.of(today, reminderTime);
                if (reminderLogRepository.existsByPatientIdAndPrescriptionIdAndScheduledTime(
                        patient.getId(), p.getId(), scheduledDateTime)) continue;

                medicines.add(p.getMedicine().getMedicineName() + " (" + p.getDosage() + ")");
                ReminderLog rl = new ReminderLog();
                rl.setPatient(patient);
                rl.setPrescription(p);
                rl.setScheduledTime(scheduledDateTime);
                rl.setStatus(ReminderStatus.NOTIFIED);
                logsToSave.add(rl);
            }
        }

        logsToSave.forEach(reminderLogRepository::save);

        if (!medicines.isEmpty()) {
            String msg = "SDP Reminder: Your medication is ready for collection — " + String.join(", ", medicines);
            notificationService.notifyPatient(patientId, msg);
            log.info("Immediate notification for patient {}: {}", patientId, medicines);
        }
    }

    boolean isInFullWindow(LocalTime reminderTime, LocalTime currentTime) {
        LocalTime windowStart = reminderTime.minusMinutes(collectionWindowMinutes);
        LocalTime windowEnd = reminderTime.plusMinutes(collectionWindowMinutes);

        // Midnight-wrap guard: skip if either boundary wraps past midnight
        if (windowStart.isAfter(reminderTime) || windowEnd.isBefore(reminderTime)) {
            return false;
        }

        return !currentTime.isBefore(windowStart) && !currentTime.isAfter(windowEnd);
    }

    boolean isInWindow(LocalTime reminderTime, LocalTime currentTime) {
        LocalTime notifyAt = reminderTime.minusMinutes(collectionWindowMinutes);

        // Midnight-wrap guard: if subtracting window wraps past midnight,
        // notifyAt will be AFTER reminderTime (e.g., 00:05 - 15min = 23:50)
        if (notifyAt.isAfter(reminderTime)) {
            return false;
        }

        return !currentTime.isBefore(notifyAt) && !currentTime.isAfter(reminderTime);
    }

    private boolean isPrescriptionValidForToday(Prescription prescription, LocalDate today) {
        if (prescription.getStartDate() != null && today.isBefore(prescription.getStartDate())) {
            return false;
        }
        if (prescription.getEndDate() != null && today.isAfter(prescription.getEndDate())) {
            return false;
        }
        return true;
    }
}
