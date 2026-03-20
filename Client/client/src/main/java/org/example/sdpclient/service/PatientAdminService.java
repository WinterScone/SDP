package org.example.sdpclient.service;

import jakarta.transaction.Transactional;
import org.example.sdpclient.dto.PatientRow;
import org.example.sdpclient.repository.AdminRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PatientAdminService {

    private final PatientRepository patientRepo;
    private final AdminRepository adminRepo;
    private final ActivityLogService activityLogService;

    public PatientAdminService(PatientRepository patientRepo, AdminRepository adminRepo,
                              ActivityLogService activityLogService) {
        this.patientRepo = patientRepo;
        this.adminRepo = adminRepo;
        this.activityLogService = activityLogService;
    }

    public List<PatientRow> getAllPatientsSafe() {
        return patientRepo.findAll()
                .stream()
                .map(p -> new PatientRow(
                        p.getId(),
                        p.getFirstName(),
                        p.getLastName(),
                        p.getDateOfBirth(),
                        p.getEmail(),
                        p.getPhone(),
                        p.getCreatedAt(),
                        p.getLinkedAdminId(),
                        p.getLinkedAdminName()
                ))
                .toList();
    }

    @Transactional
    public void linkAdminToPatient(Long patientId, Long adminId, Long assignerAdminId,
                                  String assignerAdminUsername) {
        var patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient not found"
                ));

        var admin = adminRepo.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Admin not found"
                ));

        // Check if this is a reassignment
        boolean isReassignment = patient.getLinkedAdminId() != null;
        String previousAdminName = patient.getLinkedAdminName();

        patient.setLinkedAdminId(adminId);
        patient.setLinkedAdminName(admin.getUsername());
        patientRepo.save(patient);

        // Log the activity
        String patientName = patient.getFirstName() + " " + patient.getLastName();
        activityLogService.logPatientAssigned(
                patientId,
                patientName,
                adminId,
                admin.getUsername(),
                assignerAdminId,
                assignerAdminUsername,
                isReassignment,
                previousAdminName
        );
    }
}

