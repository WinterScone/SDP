package org.example.sdpclient.service;

import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.enums.FrequencyType;
import org.example.sdpclient.repository.IntakeHistoryRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MissedIntakeNotificationService {

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

    // Fires 30 minutes after each reminder slot: 08:30, 12:30, 14:30, 16:30, 20:30
    @Scheduled(cron = "0 30 8,12,14,16,20 * * *")
    public void checkMissedIntakes() {
        int reminderHour = LocalTime.now().getHour();
        String reminderTimeLabel = String.format("%02d:00", reminderHour);
        LocalTime windowStart = LocalTime.of(reminderHour, 0);
        LocalTime windowEnd = LocalTime.now();
        LocalDate today = LocalDate.now();

        List<FrequencyType> matchingFrequencies = Arrays.stream(FrequencyType.values())
                .filter(f -> Arrays.asList(frequencyTimes(f)).contains(reminderTimeLabel))
                .toList();

        if (matchingFrequencies.isEmpty()) return;

        List<Prescription> prescriptions = prescriptionRepository.findByFrequencyIn(matchingFrequencies);

        Map<Long, List<Prescription>> byPatient = prescriptions.stream()
                .collect(Collectors.groupingBy(rx -> rx.getPatient().getId()));

        byPatient.forEach((patientId, rxList) -> {
            Patient patient = rxList.get(0).getPatient();
            Long linkedAdminId = patient.getLinkedAdmin() != null ? patient.getLinkedAdmin().getId() : null;

            List<String> missedMedicines = new ArrayList<>();
            for (Prescription rx : rxList) {
                boolean taken = intakeHistoryRepository
                        .existsByPatientIdAndMedicine_MedicineIdAndTakenDateAndTakenTimeBetween(
                                patientId,
                                rx.getMedicine().getMedicineId(),
                                today,
                                windowStart,
                                windowEnd
                        );
                if (!taken) {
                    missedMedicines.add(rx.getMedicine().getMedicineName());
                }
            }

            if (!missedMedicines.isEmpty()) {
                String patientName = patient.getFirstName() + " " + patient.getLastName();
                String missedList = String.join(", ", missedMedicines);

                if (linkedAdminId != null) {
                    notificationService.notifyAdmin(linkedAdminId,
                            "Missed intake alert: " + patientName + " has not logged their "
                            + missedList + " intake scheduled at " + reminderTimeLabel + ".");
                }

                notificationService.notifyPatient(patientId,
                        "Hi " + patient.getFirstName() + ", you missed your "
                        + missedList + " dose scheduled at " + reminderTimeLabel + ". Please take it as soon as possible.");
            }
        });
    }

    private static String[] frequencyTimes(FrequencyType frequency) {
        return switch (frequency) {
            case ONCE_A_DAY -> new String[]{"08:00"};
            case TWICE_A_DAY -> new String[]{"08:00", "20:00"};
            case THREE_TIMES_A_DAY -> new String[]{"08:00", "14:00", "20:00"};
            case FOUR_TIMES_A_DAY -> new String[]{"08:00", "12:00", "16:00", "20:00"};
        };
    }
}
