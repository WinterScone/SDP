package org.example.sdpclient.service;

import org.example.sdpclient.dto.IntakeLogRequest;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.entity.ReminderLog;
import org.example.sdpclient.entity.ReminderStatus;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.example.sdpclient.repository.ReminderLogRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class IntakeHistoryService {

    private final ReminderLogRepository reminderLogRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepo;
    private final ActivityLogService activityLogService;
    private final Clock clock;

    @Autowired
    public IntakeHistoryService(ReminderLogRepository reminderLogRepository,
                                PrescriptionRepository prescriptionRepository,
                                PatientRepository patientRepo,
                                ActivityLogService activityLogService,
                                @org.springframework.beans.factory.annotation.Value("${app.timezone:Europe/London}") String timezone) {
        this(reminderLogRepository, prescriptionRepository, patientRepo, activityLogService,
                Clock.system(ZoneId.of(timezone)));
    }

    IntakeHistoryService(ReminderLogRepository reminderLogRepository,
                         PrescriptionRepository prescriptionRepository,
                         PatientRepository patientRepo,
                         ActivityLogService activityLogService,
                         Clock clock) {
        this.reminderLogRepository = reminderLogRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepo = patientRepo;
        this.activityLogService = activityLogService;
        this.clock = clock;
    }

    public Map<String, Object> logIntake(Long patientId, IntakeLogRequest req) {
        Patient patient = patientRepo.findById(patientId).orElse(null);
        if (patient == null) {
            return Map.of("ok", false, "error", "Patient not found");
        }

        Integer medicineId;
        try {
            medicineId = Integer.parseInt(req.getMedicineId());
        } catch (NumberFormatException e) {
            return Map.of("ok", false, "error", "Invalid medicine ID");
        }

        // Find an active prescription for this patient with the given medicine
        List<Prescription> activePrescriptions = prescriptionRepository.findByPatientIdAndActiveTrue(patientId);
        Prescription prescription = activePrescriptions.stream()
                .filter(p -> p.getMedicine().getMedicineId().equals(medicineId))
                .findFirst()
                .orElse(null);

        if (prescription == null) {
            return Map.of("ok", false, "error", "No active prescription found for this medicine");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();

        // Try to find today's NOTIFIED ReminderLog for this prescription
        Optional<ReminderLog> notifiedEntry = reminderLogRepository
                .findFirstByPatientIdAndPrescription_Medicine_MedicineIdAndStatusAndScheduledTimeBetweenOrderByScheduledTimeAsc(
                        patientId, medicineId, ReminderStatus.NOTIFIED, dayStart, dayEnd);

        ReminderLog logEntry;
        if (notifiedEntry.isPresent()) {
            // Promote NOTIFIED → COLLECTED
            logEntry = notifiedEntry.get();
            logEntry.setStatus(ReminderStatus.COLLECTED);
            logEntry.setDispensedAt(now);
        } else {
            // No NOTIFIED entry — create an ad-hoc COLLECTED entry
            logEntry = new ReminderLog();
            logEntry.setPatient(patient);
            logEntry.setPrescription(prescription);
            logEntry.setScheduledTime(now);
            logEntry.setStatus(ReminderStatus.COLLECTED);
            logEntry.setDispensedAt(now);
        }

        reminderLogRepository.save(logEntry);

        String patientName = patient.getFirstName() + " " + patient.getLastName();
        activityLogService.logMedicineIntakeLogged(patient.getId(), patientName,
                prescription.getMedicine().getMedicineName());

        return Map.of("ok", true);
    }

    public List<Map<String, Object>> getHistory(Long patientId) {
        List<ReminderStatus> terminalStatuses = List.of(
                ReminderStatus.COLLECTED, ReminderStatus.MISSED);

        return reminderLogRepository
                .findByPatientIdAndStatusInOrderByScheduledTimeDesc(patientId, terminalStatuses)
                .stream()
                .map(log -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", log.getId());
                    m.put("medicineId", log.getPrescription().getMedicine().getMedicineId());
                    m.put("medicineName", log.getPrescription().getMedicine().getMedicineName());
                    m.put("takenDate", log.getScheduledTime().toLocalDate().toString());
                    m.put("takenTime", log.getScheduledTime().toLocalTime().toString());
                    m.put("status", log.getStatus().name());
                    m.put("notes", log.getFailureReason() != null ? log.getFailureReason() : "");
                    return m;
                })
                .toList();
    }
}
