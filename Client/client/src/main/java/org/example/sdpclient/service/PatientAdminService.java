package org.example.sdpclient.service;

import org.example.sdpclient.dto.PatientRow;
import org.example.sdpclient.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientAdminService {

    private final PatientRepository patientRepo;

    public PatientAdminService(PatientRepository patientRepo) {
        this.patientRepo = patientRepo;
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
}

