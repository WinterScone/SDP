package org.example.sdpclient.service;

import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.dto.PatientViewDto;
import org.example.sdpclient.entity.DispenserSlot;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.repository.DispenserSlotRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientDetailService {

    private final PatientRepository patientRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final DispenserSlotRepository dispenserSlotRepo;

    public PatientDetailService(PatientRepository patientRepo, PrescriptionRepository prescriptionRepo,
                                DispenserSlotRepository dispenserSlotRepo) {
        this.patientRepo = patientRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.dispenserSlotRepo = dispenserSlotRepo;
    }

    public List<Patient> getAllPatients() {
        return patientRepo.findAll(Sort.by("id"));
    }

    public List<Patient> getAllPatientsForAdmin(Long adminId, boolean isRoot) {
        if (isRoot) {
            return patientRepo.findAll(Sort.by("id"));
        } else {
            return patientRepo.findByLinkedAdmin_Id(adminId);
        }
    }

    public java.util.Optional<Patient> getPatientById(Long patientId) {
        return patientRepo.findById(patientId);
    }

    public boolean patientExists(Long patientId) {
        return patientRepo.existsById(patientId);
    }

    public List<PatientPrescriptionsResponse.PrescriptionItem> getPrescriptionItems(Long patientId) {
        var prescriptions = prescriptionRepo.findByPatientId(patientId);

        return prescriptions.stream()
                .map(prescription -> {
                    Integer slotNumber = dispenserSlotRepo
                            .findByMedicineAndActiveTrue(prescription.getMedicine())
                            .map(DispenserSlot::getSlotNumber)
                            .orElse(null);

                    String scheduledTime = prescription.getReminderTimes().stream()
                            .map(rt -> rt.getReminderTime().toString())
                            .collect(Collectors.joining(", "));

                    return new PatientPrescriptionsResponse.PrescriptionItem(
                            prescription.getId(),
                            slotNumber,
                            prescription.getMedicine().getMedicineId().name(),
                            prescription.getMedicine().getMedicineName(),
                            prescription.getDosage(),
                            scheduledTime.isEmpty() ? null : scheduledTime
                    );
                })
                .toList();
    }
}

