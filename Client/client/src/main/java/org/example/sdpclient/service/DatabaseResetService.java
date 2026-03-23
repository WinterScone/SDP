package org.example.sdpclient.service;

import jakarta.transaction.Transactional;
import org.example.sdpclient.repository.*;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DatabaseResetService {

    private final AdminRepository adminRepo;
    private final PatientRepository patientRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final IntakeHistoryRepository intakeHistoryRepo;
    private final ReminderLogRepository reminderLogRepo;
    private final ActivityLogService activityLogService;

    private static final Set<String> SEED_ADMIN_USERNAMES = Set.of("root", "testAdmin1", "testAdmin2");
    private static final Set<String> SEED_PATIENT_USERNAMES = Set.of("testPatient1", "testPatient2", "testPatient3");

    public DatabaseResetService(AdminRepository adminRepo,
                                PatientRepository patientRepo,
                                PrescriptionRepository prescriptionRepo,
                                IntakeHistoryRepository intakeHistoryRepo,
                                ReminderLogRepository reminderLogRepo,
                                ActivityLogService activityLogService) {
        this.adminRepo = adminRepo;
        this.patientRepo = patientRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.intakeHistoryRepo = intakeHistoryRepo;
        this.reminderLogRepo = reminderLogRepo;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public void resetToSeedData(Long adminId, String adminUsername) {
        // Get stats before clearing
        ResetStats stats = getResetStats();

        // Clear all activity logs first
        activityLogService.clearAllLogs();

        // Clear tables that have FK references to prescriptions/patients
        intakeHistoryRepo.deleteAll();
        reminderLogRepo.deleteAll();

        // Delete all prescriptions (use native queries to avoid JPA entity
        // deserialization errors from legacy frequency strings in the DB)
        prescriptionRepo.deleteAllReminderTimesNative();
        prescriptionRepo.deleteAllPrescriptionsNative();

        // Delete non-seed patients
        var allPatients = patientRepo.findAll();
        var patientsToDelete = allPatients.stream()
                .filter(p -> !SEED_PATIENT_USERNAMES.contains(p.getUsername()))
                .toList();
        patientRepo.deleteAll(patientsToDelete);

        // Reset linked admin for seed patients
        var seedPatients = patientRepo.findAll();
        seedPatients.forEach(p -> {
            p.setLinkedAdmin(null);
        });
        patientRepo.saveAll(seedPatients);

        // Delete non-seed admins
        var allAdmins = adminRepo.findAll();
        var adminsToDelete = allAdmins.stream()
                .filter(a -> !SEED_ADMIN_USERNAMES.contains(a.getUsername()))
                .toList();
        adminRepo.deleteAll(adminsToDelete);

        // Log the database reset action
        activityLogService.logDatabaseReset(
                adminId,
                adminUsername,
                (int) stats.patientsToDelete(),
                (int) stats.adminsToDelete(),
                (int) stats.prescriptionsToDelete()
        );
    }

    public ResetStats getResetStats() {
        long totalAdmins = adminRepo.count();
        long totalPatients = patientRepo.count();
        long totalPrescriptions = prescriptionRepo.count();

        long seedAdmins = SEED_ADMIN_USERNAMES.size();
        long seedPatients = SEED_PATIENT_USERNAMES.size();

        return new ResetStats(
                totalAdmins - seedAdmins,
                totalPatients - seedPatients,
                totalPrescriptions
        );
    }

    public record ResetStats(long adminsToDelete, long patientsToDelete, long prescriptionsToDelete) {}
}
