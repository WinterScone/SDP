package org.example.sdpclient.service;

import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.enums.FrequencyType;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MedicationReminderService {

    private static final Logger log = LoggerFactory.getLogger(MedicationReminderService.class);

    private final PrescriptionRepository prescriptionRepository;
    private final NotificationService notificationService;

    public MedicationReminderService(PrescriptionRepository prescriptionRepository,
                                     NotificationService notificationService) {
        this.prescriptionRepository = prescriptionRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 8,12,14,16,20 * * *")
    public void sendMedicationReminders() {
        int currentHour = LocalDateTime.now().getHour();
        log.info("Running medication reminder check for hour {}", currentHour);

        List<Prescription> activePrescriptions = prescriptionRepository.findByActiveTrue();
        Map<Long, List<String>> patientMedicines = new HashMap<>();

        for (Prescription p : activePrescriptions) {
            FrequencyType freq = FrequencyType.fromDisplayString(p.getFrequency());
            if (freq == null) {
                log.warn("Unknown frequency '{}' for prescription {}", p.getFrequency(), p.getId());
                continue;
            }

            if (freq.getDefaultHours().contains(currentHour)) {
                Long patientId = p.getPatient().getId();
                patientMedicines.computeIfAbsent(patientId, k -> new ArrayList<>())
                        .add(p.getMedicine().getMedicineName() + " (" + p.getDosage() + ")");
            }
        }

        for (Map.Entry<Long, List<String>> entry : patientMedicines.entrySet()) {
            String medicines = String.join(", ", entry.getValue());
            String message = "SDP Reminder: It's time to take your medication — " + medicines;
            notificationService.notifyPatient(entry.getKey(), message);
        }

        log.info("Medication reminders sent to {} patients", patientMedicines.size());
    }
}
