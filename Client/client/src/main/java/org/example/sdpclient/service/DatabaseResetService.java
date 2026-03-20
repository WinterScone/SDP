package org.example.sdpclient.service;

import jakarta.transaction.Transactional;
import org.example.sdpclient.repository.AdminRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class DatabaseResetService {

    private final AdminRepository adminRepo;
    private final PatientRepository patientRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final ActivityLogService activityLogService;

    private static final Set<String> SEED_ADMIN_USERNAMES = Set.of("root", "testAdmin1", "testAdmin2");
    private static final Set<String> SEED_PATIENT_USERNAMES = Set.of("testPatient1", "testPatient2");

    public DatabaseResetService(AdminRepository adminRepo,
                                PatientRepository patientRepo,
                                PrescriptionRepository prescriptionRepo,
                                ActivityLogService activityLogService) {
        this.adminRepo = adminRepo;
        this.patientRepo = patientRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public void resetToSeedData(Long adminId, String adminUsername) {
        // Get stats before clearing
        ResetStats stats = getResetStats();

        // Clear all activity logs first
        activityLogService.clearAllLogs();

        // Delete all prescriptions first (foreign key constraint)
        prescriptionRepo.deleteAll();

        // Delete non-seed patients
        var allPatients = patientRepo.findAll();
        var patientsToDelete = allPatients.stream()
                .filter(p -> !SEED_PATIENT_USERNAMES.contains(p.getUsername()))
                .toList();
        patientRepo.deleteAll(patientsToDelete);

        // Reset linked admin for seed patients
        var seedPatients = patientRepo.findAll();
        seedPatients.forEach(p -> {
            p.setLinkedAdminId(null);
            p.setLinkedAdminName(null);
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
