package org.example.sdpclient.service;

import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.enums.FrequencyType;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MedicationReminderService {

    private final PrescriptionRepository prescriptionRepository;
    private final NotificationService notificationService;

    public MedicationReminderService(PrescriptionRepository prescriptionRepository,
                                     NotificationService notificationService) {
        this.prescriptionRepository = prescriptionRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 8,12,14,16,20 * * *")
    public void sendReminders() {
        String timeLabel = String.format("%02d:00", LocalTime.now().getHour());

        List<FrequencyType> matchingFrequencies = Arrays.stream(FrequencyType.values())
                .filter(f -> Arrays.asList(frequencyTimes(f)).contains(timeLabel))
                .toList();

        if (matchingFrequencies.isEmpty()) return;

        List<Prescription> prescriptions = prescriptionRepository.findByFrequencyIn(matchingFrequencies);

        Map<Long, List<Prescription>> byPatient = prescriptions.stream()
                .collect(Collectors.groupingBy(rx -> rx.getPatient().getId()));

        byPatient.forEach((patientId, rxList) -> {
            String firstName = rxList.get(0).getPatient().getFirstName();
            String sms = buildReminderSms(firstName, timeLabel, rxList);
            notificationService.notifyPatient(patientId, sms);
        });
    }

    private String buildReminderSms(String firstName, String time, List<Prescription> prescriptions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(firstName).append(", your medication at ")
          .append(time).append(" is ready to collect.");
        for (Prescription rx : prescriptions) {
            int tablets = calculateTablets(rx.getDosage(), rx.getMedicine().getDosagePerForm());
            sb.append("\n- ").append(rx.getMedicine().getMedicineName())
              .append(", ").append(rx.getDosage()).append("mg")
              .append(", ").append(tablets).append(" tablet(s)");
        }
        return sb.toString();
    }

    private static String[] frequencyTimes(FrequencyType frequency) {
        return switch (frequency) {
            case ONCE_A_DAY -> new String[]{"08:00"};
            case TWICE_A_DAY -> new String[]{"08:00", "20:00"};
            case THREE_TIMES_A_DAY -> new String[]{"08:00", "14:00", "20:00"};
            case FOUR_TIMES_A_DAY -> new String[]{"08:00", "12:00", "16:00", "20:00"};
        };
    }

    private static int calculateTablets(String dosage, Integer dosagePerForm) {
        if (dosagePerForm == null || dosagePerForm == 0) return 1;
        try {
            return (int) Math.ceil((double) Integer.parseInt(dosage) / dosagePerForm);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
