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

    public PatientAdminService(PatientRepository patientRepo, AdminRepository adminRepo) {
        this.patientRepo = patientRepo;
        this.adminRepo = adminRepo;
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
    public void linkAdminToPatient(Long patientId, Long adminId) {
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

        patient.setLinkedAdminId(adminId);
        patient.setLinkedAdminName(admin.getUsername());
        patientRepo.save(patient);
    }
}

