package org.example.sdpclient.service;

import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.enums.FrequencyType;
import org.example.sdpclient.repository.IntakeHistoryRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class MissedIntakeNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MissedIntakeNotificationService.class);

    private final PrescriptionRepository prescriptionRepository;
    private final IntakeHistoryRepository intakeHistoryRepository;
    private final NotificationService notificationService;

    public MissedIntakeNotificationService(PrescriptionRepository prescriptionRepository,
                                           IntakeHistoryRepository intakeHistoryRepository,
                                           NotificationService notificationService) {
        this.prescriptionRepository = prescriptionRepository;
        this.intakeHistoryRepository = intakeHistoryRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 30 9,13,15,17,21 * * *")
    public void checkMissedIntakes() {
        int currentHour = LocalTime.now().getHour();
        int reminderHour = currentHour - 1; // check for the reminder that went out ~90 min ago
        // Map 9->8, 13->12, 15->14, 17->16, 21->20
        log.info("Checking missed intakes for reminder hour {}", reminderHour);

        List<Prescription> activePrescriptions = prescriptionRepository.findByActiveTrue();
        LocalDate today = LocalDate.now();

        for (Prescription p : activePrescriptions) {
            FrequencyType freq = FrequencyType.fromDisplayString(p.getFrequency());
            if (freq == null) continue;

            if (!freq.getDefaultHours().contains(reminderHour)) continue;

            Patient patient = p.getPatient();
            LocalTime windowStart = LocalTime.of(reminderHour, 0);
            LocalTime windowEnd = LocalTime.of(currentHour, 30);

            boolean taken = intakeHistoryRepository
                    .existsByPatient_IdAndMedicine_MedicineIdAndTakenDateAndTakenTimeBetween(
                            patient.getId(), p.getMedicine().getMedicineId(),
                            today, windowStart, windowEnd);

            if (!taken) {
                String medicineName = p.getMedicine().getMedicineName();
                notificationService.notifyPatient(patient.getId(),
                        "SDP Alert: You missed your " + medicineName + " dose scheduled for "
                                + windowStart + ". Please take it as soon as possible.");

                if (patient.getLinkedAdmin() != null) {
                    notificationService.notifyAdmin(patient.getLinkedAdmin().getId(),
                            "SDP Alert: Patient " + patient.getFirstName() + " " + patient.getLastName()
                                    + " missed their " + medicineName + " dose scheduled for " + windowStart + ".");
                }
            }
        }
    }
}
