package org.example.sdpclient.service;

import org.example.sdpclient.dto.DispenseResultRequest;
import org.example.sdpclient.dto.DispenseResultResponse;
import org.example.sdpclient.dto.MachineIdentifyResponse;
import org.example.sdpclient.dto.MachinePatientImageItem;
import org.example.sdpclient.dto.MachinePatientImagesResponse;
import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.entity.PrescriptionReminderTime;
import org.example.sdpclient.entity.ReminderLog;
import org.example.sdpclient.entity.ReminderStatus;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.example.sdpclient.repository.ReminderLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class MachineService {

    private final PatientFaceService patientFaceService;
    private final PrescriptionRepository prescriptionRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final PatientRepository patientRepository;

    public MachineService(
            PatientFaceService patientFaceService,
            PrescriptionRepository prescriptionRepository,
            ReminderLogRepository reminderLogRepository,
            PatientRepository patientRepository
    ) {
        this.patientFaceService = patientFaceService;
        this.prescriptionRepository = prescriptionRepository;
        this.reminderLogRepository = reminderLogRepository;
        this.patientRepository = patientRepository;
    }

    public MachinePatientImagesResponse getAllPatientImages() {
        List<MachinePatientImageItem> items = patientRepository.findByFaceActiveTrueAndFaceDataIsNotNull()
                .stream()
                .filter(p -> p.getFaceData() != null && p.getFaceData().length > 0)
                .map(p -> new MachinePatientImageItem(
                        p.getId(),
                        p.getFaceContentType() != null ? p.getFaceContentType() : "image/jpeg",
                        Base64.getEncoder().encodeToString(p.getFaceData()),
                        p.getFaceEnrolledAt() != null ? p.getFaceEnrolledAt().toString() : null
                ))
                .toList();

        return new MachinePatientImagesResponse(
                true,
                items.size(),
                items,
                items.isEmpty()
                        ? "No enrolled patient face images found"
                        : "Patient face images loaded successfully"
        );
    }

    public MachineIdentifyResponse identifyPatientAndLoadPrescriptions(byte[] imageBytes) {
        Patient patient = patientFaceService.identifyPatient(imageBytes);
        if (patient == null) {
            return new MachineIdentifyResponse(
                    true,
                    false,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    "No matching patient found"
            );
        }

        return buildDuePrescriptionResponse(patient);
    }

    public MachineIdentifyResponse getPatientPrescriptionsByPatientId(Long patientId) {
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient == null) {
            return new MachineIdentifyResponse(
                    false,
                    false,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    "Patient not found"
            );
        }

        return buildDuePrescriptionResponse(patient);
    }

    public DispenseResultResponse saveDispenseResult(DispenseResultRequest request) {
        if (request.getPatientId() == null) {
            return new DispenseResultResponse(false, "patientId is required");
        }
        if (request.getPrescriptionId() == null) {
            return new DispenseResultResponse(false, "prescriptionId is required");
        }
        if (request.getScheduledTime() == null || request.getScheduledTime().isBlank()) {
            return new DispenseResultResponse(false, "scheduledTime is required");
        }
        if (request.getStatus() == null || request.getStatus().isBlank()) {
            return new DispenseResultResponse(false, "status is required");
        }

        Patient patient = patientRepository.findById(request.getPatientId()).orElse(null);
        if (patient == null) {
            return new DispenseResultResponse(false, "Patient not found");
        }

        Prescription prescription = prescriptionRepository.findById(request.getPrescriptionId()).orElse(null);
        if (prescription == null) {
            return new DispenseResultResponse(false, "Prescription not found");
        }

        if (!prescription.getPatient().getId().equals(patient.getId())) {
            return new DispenseResultResponse(false, "Prescription does not belong to patient");
        }

        LocalDateTime scheduledDateTime;
        try {
            scheduledDateTime = LocalDateTime.parse(request.getScheduledTime());
        } catch (Exception e) {
            return new DispenseResultResponse(false, "scheduledTime must be ISO format like 2026-03-17T08:00:00");
        }

        ReminderStatus status;
        try {
            status = ReminderStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception e) {
            return new DispenseResultResponse(false, "Invalid status. Use TAKEN, MISSED, FAILED, or SKIPPED");
        }

        ReminderLog log = reminderLogRepository
                .findByPatientIdAndPrescriptionIdAndScheduledTime(
                        patient.getId(),
                        prescription.getId(),
                        scheduledDateTime
                )
                .orElseGet(ReminderLog::new);

        log.setPatient(patient);
        log.setPrescription(prescription);
        log.setScheduledTime(scheduledDateTime);
        log.setStatus(status);
        log.setFailureReason(request.getFailureReason());

        if (status == ReminderStatus.TAKEN) {
            log.setDispensedAt(LocalDateTime.now());
        } else {
            log.setDispensedAt(null);
        }

        reminderLogRepository.save(log);
        return new DispenseResultResponse(true, "Dispense result saved successfully");
    }

    private MachineIdentifyResponse buildDuePrescriptionResponse(Patient patient) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<PatientPrescriptionsResponse.PrescriptionItem> dueItems = new ArrayList<>();

        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdAndActiveTrue(patient.getId());

        for (Prescription prescription : prescriptions) {
            if (!isPrescriptionValidForToday(prescription, today)) {
                continue;
            }

            for (PrescriptionReminderTime reminderTime : prescription.getReminderTimes()) {
                if (!isDueNow(reminderTime.getReminderTime(), now)) {
                    continue;
                }

                LocalDateTime scheduledDateTime = LocalDateTime.of(today, reminderTime.getReminderTime());

                boolean alreadyLogged = reminderLogRepository
                        .findByPatientIdAndPrescriptionIdAndScheduledTime(
                                patient.getId(),
                                prescription.getId(),
                                scheduledDateTime
                        )
                        .isPresent();

                if (alreadyLogged) {
                    continue;
                }

                dueItems.add(
                        new PatientPrescriptionsResponse.PrescriptionItem(
                                prescription.getId(),
                                prescription.getMedicine().getMedicineId().getId(),
                                prescription.getMedicine().getMedicineId().name(),
                                prescription.getMedicine().getMedicineName(),
                                prescription.getDosage(),
                                prescription.getFrequency(),
                                reminderTime.getReminderTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                        )
                );
            }
        }

        String patientName = (patient.getFirstName() == null ? "" : patient.getFirstName().trim()) + " "
                + (patient.getLastName() == null ? "" : patient.getLastName().trim());
        patientName = patientName.trim();

        return new MachineIdentifyResponse(
                true,
                true,
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patientName,
                dueItems,
                dueItems.isEmpty()
                        ? "Patient identified successfully, but no medicine is due now"
                        : "Patient identified successfully"
        );
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
