package org.example.sdpclient.service;

import org.example.sdpclient.dto.DispenseResultRequest;
import org.example.sdpclient.dto.DispenseResultResponse;
import org.example.sdpclient.dto.MachineIdentifyResponse;
import org.example.sdpclient.dto.MachinePatientImageItem;
import org.example.sdpclient.dto.MachinePatientImagesResponse;
import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.entity.ReminderLog;
import org.example.sdpclient.entity.ReminderStatus;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.example.sdpclient.repository.ReminderLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class MachineService {

    private final PatientFaceService patientFaceService;
    private final PrescriptionRepository prescriptionRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final PatientRepository patientRepository;
    private final ActivityLogService activityLogService;
    private final PatientDetailService patientDetailService;

    public MachineService(
            PatientFaceService patientFaceService,
            PrescriptionRepository prescriptionRepository,
            ReminderLogRepository reminderLogRepository,
            PatientRepository patientRepository,
            ActivityLogService activityLogService,
            PatientDetailService patientDetailService
    ) {
        this.patientFaceService = patientFaceService;
        this.prescriptionRepository = prescriptionRepository;
        this.reminderLogRepository = reminderLogRepository;
        this.patientRepository = patientRepository;
        this.activityLogService = activityLogService;
        this.patientDetailService = patientDetailService;
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
        String rawStatus = request.getStatus().toUpperCase();
        switch (rawStatus) {
            case "TAKEN", "COLLECTED" -> status = ReminderStatus.COLLECTED;
            case "FAILED", "SKIPPED", "MISSED" -> status = ReminderStatus.MISSED;
            default -> {
                return new DispenseResultResponse(false,
                        "Invalid status. Use COLLECTED, MISSED, or legacy: TAKEN, FAILED, SKIPPED");
            }
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

        if (status == ReminderStatus.COLLECTED) {
            log.setDispensedAt(LocalDateTime.now());
        } else {
            log.setDispensedAt(null);
        }

        reminderLogRepository.save(log);

        String patientName = (patient.getFirstName() != null ? patient.getFirstName() : "") + " "
                + (patient.getLastName() != null ? patient.getLastName() : "");
        activityLogService.logMedicineDispensed(
                patient.getId(),
                patientName.trim(),
                prescription.getMedicine().getMedicineName(),
                status.name()
        );

        return new DispenseResultResponse(true, "Dispense result saved successfully");
    }

    private MachineIdentifyResponse buildDuePrescriptionResponse(Patient patient) {
        List<PatientPrescriptionsResponse.PrescriptionItem> dueItems =
                patientDetailService.getCollectableItems(patient.getId());

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
}
