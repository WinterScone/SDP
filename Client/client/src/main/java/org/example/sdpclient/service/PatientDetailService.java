package org.example.sdpclient.service;

import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.dto.PatientViewDto;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientDetailService {

    private final PatientRepository patientRepo;
    private final PrescriptionRepository prescriptionRepo;

    public PatientDetailService(PatientRepository patientRepo, PrescriptionRepository prescriptionRepo) {
        this.patientRepo = patientRepo;
        this.prescriptionRepo = prescriptionRepo;
    }

    public List<Patient> getAllPatients() {
        return patientRepo.findAll(Sort.by("id"));
    }

    public List<Patient> getAllPatientsForAdmin(Long adminId, boolean isRoot) {
        if (isRoot) {
            return patientRepo.findAll(Sort.by("id"));
        } else {
            return patientRepo.findByLinkedAdminId(adminId);
        }
    }

    public boolean patientExists(Long patientId) {
        return patientRepo.existsById(patientId);
    }

    public List<PatientPrescriptionsResponse.PrescriptionItem> getPrescriptionItems(Long patientId) {
        var prescriptions = prescriptionRepo.findByPatientId(patientId);

        return prescriptions.stream()
                .map(prescription -> new PatientPrescriptionsResponse.PrescriptionItem(
                        prescription.getMedicine().getMedicineId().name(),
                        prescription.getMedicine().getMedicineName(),
                        prescription.getDosage(),
                        prescription.getFrequency()
                ))
                .toList();
    }
}

