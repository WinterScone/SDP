package org.example.sdpclient.service;

import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.dto.PatientViewDto;
import org.example.sdpclient.entity.DispenserSlot;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.entity.PrescriptionReminderTime;
import org.example.sdpclient.entity.ReminderStatus;
import org.example.sdpclient.repository.DispenserSlotRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.example.sdpclient.repository.ReminderLogRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientDetailService {

    private final PatientRepository patientRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final DispenserSlotRepository dispenserSlotRepo;
    private final ReminderLogRepository reminderLogRepo;

    public PatientDetailService(PatientRepository patientRepo, PrescriptionRepository prescriptionRepo,
                                DispenserSlotRepository dispenserSlotRepo, ReminderLogRepository reminderLogRepo) {
        this.patientRepo = patientRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.dispenserSlotRepo = dispenserSlotRepo;
        this.reminderLogRepo = reminderLogRepo;
    }

    public List<Patient> getAllPatients() {
        return patientRepo.findAll(Sort.by("id"));
    }

    public List<Patient> getAllPatientsForAdmin(Long adminId, boolean isRoot) {
        if (isRoot) {
            return patientRepo.findAll(Sort.by("id"));
        } else {
            return patientRepo.findByLinkedAdmin_Id(adminId);
        }
    }

    public java.util.Optional<Patient> getPatientById(Long patientId) {
        return patientRepo.findById(patientId);
    }

    public boolean patientExists(Long patientId) {
        return patientRepo.existsById(patientId);
    }

    public List<PatientPrescriptionsResponse.PrescriptionItem> getPrescriptionItems(Long patientId) {
        var prescriptions = prescriptionRepo.findByPatientId(patientId);

        return prescriptions.stream()
                .map(prescription -> {
                    Integer slotNumber = dispenserSlotRepo
                            .findByMedicineAndActiveTrue(prescription.getMedicine())
                            .map(DispenserSlot::getSlotNumber)
                            .orElse(null);

                    String scheduledTime = prescription.getReminderTimes().stream()
                            .map(rt -> rt.getReminderTime().toString())
                            .collect(Collectors.joining(", "));

                    return new PatientPrescriptionsResponse.PrescriptionItem(
                            prescription.getId(),
                            slotNumber,
                            String.valueOf(prescription.getMedicine().getMedicineId()),
                            prescription.getMedicine().getMedicineName(),
                            prescription.getDosage(),
                            prescription.getFrequency(),
                            scheduledTime.isEmpty() ? null : scheduledTime
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PatientPrescriptionsResponse.PrescriptionItem> getCollectableItems(Long patientId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<PatientPrescriptionsResponse.PrescriptionItem> dueItems = new ArrayList<>();

        List<Prescription> prescriptions = prescriptionRepo.findByPatientIdAndActiveTrue(patientId);

        for (Prescription prescription : prescriptions) {
            if (!isPrescriptionValidForToday(prescription, today)) {
                continue;
            }

            for (PrescriptionReminderTime reminderTime : prescription.getReminderTimes()) {
                if (!isDueNow(reminderTime.getReminderTime(), now)) {
                    continue;
                }

                LocalDateTime scheduledDateTime = LocalDateTime.of(today, reminderTime.getReminderTime());

                boolean alreadyDispensed = reminderLogRepo
                        .findByPatientIdAndPrescriptionIdAndScheduledTime(
                                patientId,
                                prescription.getId(),
                                scheduledDateTime
                        )
                        .filter(log -> log.getStatus() != ReminderStatus.NOTIFIED)
                        .isPresent();

                if (alreadyDispensed) {
                    continue;
                }

                Integer slotNumber = dispenserSlotRepo
                        .findByMedicineAndActiveTrue(prescription.getMedicine())
                        .map(DispenserSlot::getSlotNumber)
                        .orElse(null);

                dueItems.add(new PatientPrescriptionsResponse.PrescriptionItem(
                        prescription.getId(),
                        slotNumber,
                        String.valueOf(prescription.getMedicine().getMedicineId()),
                        prescription.getMedicine().getMedicineName(),
                        prescription.getDosage(),
                        reminderTime.getReminderTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                ));
            }
        }

        return dueItems;
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

    private boolean isDueNow(LocalTime scheduledTime, LocalTime now) {
        int windowMinutes = 15;
        LocalTime start = scheduledTime.minusMinutes(windowMinutes);
        LocalTime end = scheduledTime.plusMinutes(windowMinutes);
        return !now.isBefore(start) && !now.isAfter(end);
    }
}
