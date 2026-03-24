package org.example.sdpclient.service;

import org.example.sdpclient.dto.ActivityLogDto;
import org.example.sdpclient.entity.ActivityLog;
import org.example.sdpclient.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    private final ActivityLogRepository repository;

    public ActivityLogService(ActivityLogRepository repository) {
        this.repository = repository;
    }

    public List<ActivityLogDto> getAllLogs() {
        return repository.findAllByOrderByTimestampDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void logMedicineStockChange(String medicineName, int oldQuantity, int newQuantity,
                                       Long adminId, String adminUsername) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("MEDICINE_STOCK_CHANGE");
        log.setDescription(String.format("Medicine stock updated: %s from %d to %d",
                                        medicineName, oldQuantity, newQuantity));
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        log.setMedicineName(medicineName);
        log.setAdditionalDetails(String.format("Old quantity: %d, New quantity: %d",
                                               oldQuantity, newQuantity));
        repository.save(log);
    }

    @Transactional
    public void logPrescriptionCreated(Long patientId, String patientName, String medicineName,
                                      String dosage, String frequency,
                                      Long adminId, String adminUsername) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("PRESCRIPTION_CREATED");
        log.setDescription(String.format("New prescription added for %s: %s (%s, %s)",
                                        patientName, medicineName, dosage, frequency));
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        log.setPatientId(patientId);
        log.setPatientName(patientName);
        log.setMedicineName(medicineName);
        log.setAdditionalDetails(String.format("Dosage: %s, Frequency: %s", dosage, frequency));
        repository.save(log);
    }

    @Transactional
    public void logPrescriptionDeleted(Long patientId, String patientName, String medicineName,
                                      String dosage, String frequency,
                                      Long adminId, String adminUsername) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("PRESCRIPTION_DELETED");
        log.setDescription(String.format("Prescription deleted for %s: %s (%s, %s)",
                                        patientName, medicineName, dosage, frequency));
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        log.setPatientId(patientId);
        log.setPatientName(patientName);
        log.setMedicineName(medicineName);
        log.setAdditionalDetails(String.format("Dosage: %s, Frequency: %s", dosage, frequency));
        repository.save(log);
    }

    @Transactional
    public void logAdminCreated(String newAdminUsername, String newAdminFullName,
                               Long creatorAdminId, String creatorAdminUsername) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("ADMIN_CREATED");
        log.setDescription(String.format("New admin created: %s (%s)",
                                        newAdminUsername, newAdminFullName));
        log.setAdminId(creatorAdminId);
        log.setAdminUsername(creatorAdminUsername);
        log.setAdditionalDetails(String.format("New admin: %s (%s)",
                                               newAdminUsername, newAdminFullName));
        repository.save(log);
    }

    @Transactional
    public void logPatientAssigned(Long patientId, String patientName,
                                  Long adminId, String adminUsername,
                                  Long assignerAdminId, String assignerAdminUsername,
                                  boolean isReassignment, String previousAdminName) {
        ActivityLog log = new ActivityLog();

        if (isReassignment) {
            log.setActivityType("PATIENT_REASSIGNED");
            log.setDescription(String.format("Patient %s reassigned from %s to %s",
                                            patientName, previousAdminName, adminUsername));
            log.setAdditionalDetails(String.format("Previous admin: %s, New admin: %s",
                                                   previousAdminName, adminUsername));
        } else {
            log.setActivityType("PATIENT_ASSIGNED");
            log.setDescription(String.format("Patient %s assigned to admin %s",
                                            patientName, adminUsername));
            log.setAdditionalDetails(String.format("Assigned to: %s", adminUsername));
        }

        log.setAdminId(assignerAdminId);
        log.setAdminUsername(assignerAdminUsername);
        log.setPatientId(patientId);
        log.setPatientName(patientName);
        repository.save(log);
    }

    @Transactional
    public void logDatabaseReset(Long adminId, String adminUsername, int patientsDeleted,
                                int adminsDeleted, int prescriptionsDeleted) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("DATABASE_RESET");
        log.setDescription("Database reset successful.");
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        log.setAdditionalDetails(String.format("Admins deleted: %d, Patients deleted: %d, Prescriptions deleted: %d",
                                               adminsDeleted, patientsDeleted, prescriptionsDeleted));
        repository.save(log);
    }

    @Transactional
    public void logPatientUpdated(Long patientId, String patientName, Long adminId, String adminUsername) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("PATIENT_UPDATED");
        log.setDescription(String.format("Patient details updated for %s", patientName));
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        log.setPatientId(patientId);
        log.setPatientName(patientName);
        repository.save(log);
    }

    // --- P1: Security & Compliance ---

    @Transactional
    public void logAdminLogin(Long adminId, String adminUsername) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("ADMIN_LOGIN");
        log.setDescription(String.format("Admin logged in: %s", adminUsername));
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        repository.save(log);
    }

    @Transactional
    public void logAdminLoginFailed(String attemptedUsername) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("ADMIN_LOGIN_FAILED");
        log.setDescription(String.format("Failed admin login attempt for username: %s", attemptedUsername));
        log.setAdditionalDetails(String.format("Attempted username: %s", attemptedUsername));
        repository.save(log);
    }

    @Transactional
    public void logPatientSignup(Long patientId, String patientName) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("PATIENT_SIGNUP");
        log.setDescription(String.format("New patient registered: %s", patientName));
        log.setPatientId(patientId);
        log.setPatientName(patientName);
        repository.save(log);
    }

    @Transactional
    public void logPrescriptionUpdated(Long patientId, String patientName, String medicineName,
                                       String dosage, String frequency,
                                       Long adminId, String adminUsername) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("PRESCRIPTION_UPDATED");
        log.setDescription(String.format("Prescription updated for %s: %s (%s, %s)",
                                        patientName, medicineName, dosage, frequency));
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        log.setPatientId(patientId);
        log.setPatientName(patientName);
        log.setMedicineName(medicineName);
        log.setAdditionalDetails(String.format("Dosage: %s, Frequency: %s", dosage, frequency));
        repository.save(log);
    }

    // --- P2: Operational Visibility ---

    @Transactional
    public void logFaceEnrolled(Long patientId, String patientName) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("FACE_ENROLLED");
        log.setDescription(String.format("Face enrolled for patient: %s", patientName));
        log.setPatientId(patientId);
        log.setPatientName(patientName);
        repository.save(log);
    }

    @Transactional
    public void logMedicineDispensed(Long patientId, String patientName,
                                     String medicineName, String status) {
        ActivityLog log = new ActivityLog();
        if ("TAKEN".equalsIgnoreCase(status)) {
            log.setActivityType("MEDICINE_DISPENSED");
            log.setDescription(String.format("Medicine dispensed to %s: %s", patientName, medicineName));
        } else {
            log.setActivityType("MEDICINE_DISPENSE_FAILED");
            log.setDescription(String.format("Medicine dispense %s for %s: %s",
                                            status.toLowerCase(), patientName, medicineName));
        }
        log.setPatientId(patientId);
        log.setPatientName(patientName);
        log.setMedicineName(medicineName);
        log.setAdditionalDetails(String.format("Status: %s", status));
        repository.save(log);
    }

    // --- P3: Completeness ---

    @Transactional
    public void logPatientLogin(Long patientId, String patientName) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("PATIENT_LOGIN");
        log.setDescription(String.format("Patient logged in: %s", patientName));
        log.setPatientId(patientId);
        log.setPatientName(patientName);
        repository.save(log);
    }

    @Transactional
    public void logPatientLoginFailed(String attemptedUsername) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("PATIENT_LOGIN_FAILED");
        log.setDescription(String.format("Failed patient login attempt for username: %s", attemptedUsername));
        log.setAdditionalDetails(String.format("Attempted username: %s", attemptedUsername));
        repository.save(log);
    }

    @Transactional
    public void logAdminLogout(Long adminId, String adminUsername) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("ADMIN_LOGOUT");
        log.setDescription(String.format("Admin logged out: %s", adminUsername));
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        repository.save(log);
    }

    @Transactional
    public void logMedicineIntakeLogged(Long patientId, String patientName, String medicineName) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("MEDICINE_INTAKE_LOGGED");
        log.setDescription(String.format("Medicine intake logged for %s: %s", patientName, medicineName));
        log.setPatientId(patientId);
        log.setPatientName(patientName);
        log.setMedicineName(medicineName);
        repository.save(log);
    }

    @Transactional
    public void logMessageSent(Long adminId, String adminUsername, int recipientCount) {
        ActivityLog log = new ActivityLog();
        log.setActivityType("MESSAGE_SENT");
        log.setDescription(String.format("SMS message sent to %d recipient(s) by %s",
                recipientCount, adminUsername));
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        log.setAdditionalDetails(String.format("Recipients: %d", recipientCount));
        repository.save(log);
    }

    @Transactional
    public void clearAllLogs() {
        repository.deleteAll();
    }

    private ActivityLogDto toDto(ActivityLog log) {
        return new ActivityLogDto(
                log.getId(),
                log.getActivityType(),
                log.getDescription(),
                log.getAdminId(),
                log.getAdminUsername(),
                log.getPatientId(),
                log.getPatientName(),
                log.getMedicineName(),
                log.getAdditionalDetails(),
                log.getTimestamp()
        );
    }
}
