package org.example.sdpclient.service;

import jakarta.transaction.Transactional;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.repository.*;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DatabaseResetService {

    private final AdminRepository adminRepo;
    private final PatientRepository patientRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final DispenserSlotRepository dispenserSlotRepo;
    private final MedicineRepository medicineRepo;
    private final PatientImageRepository patientImageRepo;
    private final ReminderLogRepository reminderLogRepo;
    private final ActivityLogService activityLogService;

    private static final Set<String> SEED_ADMIN_USERNAMES = Set.of("root", "testAdmin1", "testAdmin2");
    private static final Set<String> SEED_PATIENT_USERNAMES = Set.of("testPatient1", "testPatient2", "testPatient3", "testPatient4", "testPatient5");

    public DatabaseResetService(AdminRepository adminRepo,
                                PatientRepository patientRepo,
                                PrescriptionRepository prescriptionRepo,
                                DispenserSlotRepository dispenserSlotRepo,
                                MedicineRepository medicineRepo,
                                PatientImageRepository patientImageRepo,
                                ReminderLogRepository reminderLogRepo,
                                ActivityLogService activityLogService) {
        this.adminRepo = adminRepo;
        this.patientRepo = patientRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.dispenserSlotRepo = dispenserSlotRepo;
        this.medicineRepo = medicineRepo;
        this.patientImageRepo = patientImageRepo;
        this.reminderLogRepo = reminderLogRepo;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public void resetToSeedData(Long adminId, String adminUsername) {
        ResetStats stats = getResetStats();

        // Clear all logs
        activityLogService.clearAllLogs();
        reminderLogRepo.deleteAllNative();

        // Delete all prescriptions (use native queries to avoid JPA entity
        // deserialization errors from legacy frequency strings in the DB)
        prescriptionRepo.deleteAllReminderTimesNative();
        prescriptionRepo.deleteAllPrescriptionsNative();

        // Clear dispenser slots
        dispenserSlotRepo.deleteAll();

        // Clear patient images
        patientImageRepo.deleteAll();

        // Delete non-seed patients
        var patientsToDelete = patientRepo.findAll().stream()
                .filter(p -> !SEED_PATIENT_USERNAMES.contains(p.getUsername()))
                .toList();
        patientRepo.deleteAll(patientsToDelete);

        // Reset seed patients to clean state
        patientRepo.findAll().forEach(p -> {
            p.setLinkedAdmin(null);
            p.setFaceData(null);
            p.setFaceContentType(null);
            p.setFaceEnrolledAt(null);
            p.setFaceActive(false);
            p.setSmsConsent(false);
        });
        patientRepo.saveAll(patientRepo.findAll());

        // Delete non-seed admins
        var adminsToDelete = adminRepo.findAll().stream()
                .filter(a -> !SEED_ADMIN_USERNAMES.contains(a.getUsername()))
                .toList();
        adminRepo.deleteAll(adminsToDelete);

        // Reset all medicine stock to 0
        for (Medicine m : medicineRepo.findAll()) {
            m.setQuantity(0);
        }
        medicineRepo.saveAll(medicineRepo.findAll());

        // Log the reset
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
