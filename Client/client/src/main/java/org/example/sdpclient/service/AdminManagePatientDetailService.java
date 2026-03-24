package org.example.sdpclient.service;

import org.example.sdpclient.dto.*;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.entity.PrescriptionReminderTime;
import org.example.sdpclient.repository.MedicineRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AdminManagePatientDetailService {

    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;
    private final ActivityLogService activityLogService;

    public AdminManagePatientDetailService(PatientRepository patientRepository,
                                           PrescriptionRepository prescriptionRepository,
                                           MedicineRepository medicineRepository,
                                           ActivityLogService activityLogService) {
        this.patientRepository = patientRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.medicineRepository = medicineRepository;
        this.activityLogService = activityLogService;
    }


    public List<PatientViewDto> searchPatients(String q, Long adminId, boolean isRoot) {
        String keyword = q == null ? "" : q.trim();
        if (keyword.isEmpty()) {
            return List.of();
        }

        List<Patient> patients;
        if (isRoot) {
            patients = patientRepository.searchByKeyword(keyword);
        } else {
            patients = patientRepository.searchByKeywordAndLinkedAdmin(keyword, adminId);
        }

        return patients.stream()
                .map(p -> new PatientViewDto(
                        p.getId(),
                        p.getFirstName(),
                        p.getLastName(),
                        p.getDateOfBirth(),
                        p.getEmail(),
                        p.getPhone(),
                        p.isSmsConsent(),
                        p.isFaceActive(),
                        p.getLinkedAdmin() != null ? p.getLinkedAdmin().getUsername() : null,
                        List.of()
                ))
                .toList();
    }

    public boolean canAdminAccessPatient(Long patientId, Long adminId, boolean isRoot) {
        if (isRoot) {
            return true;
        }
        Optional<Patient> patient = patientRepository.findById(patientId);
        return patient.isPresent()
                && patient.get().getLinkedAdmin() != null
                && adminId.equals(patient.get().getLinkedAdmin().getId());
    }

    public Optional<Patient> findPatient(Long id) {
        return patientRepository.findById(id);
    }

    public PatientViewDto updatePatientDetails(Long patientId, PatientUpdateDto dto, Long adminId, String adminUsername) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

        if (dto.getFirstName() != null && !dto.getFirstName().isBlank()) patient.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null && !dto.getLastName().isBlank()) patient.setLastName(dto.getLastName());
        if (dto.getDateOfBirth() != null) patient.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getEmail() != null) patient.setEmail(dto.getEmail().isBlank() ? null : dto.getEmail());
        if (dto.getPhone() != null) patient.setPhone(dto.getPhone().isBlank() ? null : dto.getPhone());
        if (dto.getSmsConsent() != null) patient.setSmsConsent(dto.getSmsConsent());
        if (dto.getFaceRecognitionConsent() != null) patient.setFaceRecognitionConsent(dto.getFaceRecognitionConsent());

        patientRepository.save(patient);
        activityLogService.logPatientUpdated(patientId,
                patient.getFirstName() + " " + patient.getLastName(), adminId, adminUsername);

        return new PatientViewDto(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth(),
                patient.getEmail(),
                patient.getPhone(),
                patient.isSmsConsent(),
                patient.isFaceActive(),
                patient.getLinkedAdmin() != null ? patient.getLinkedAdmin().getUsername() : null,
                getPrescriptionViews(patientId)
        );
    }


    public List<PrescriptionViewDto> getPrescriptionViews(Long patientId) {
        return prescriptionRepository.findByPatientId(patientId).stream()
                .map(rx -> new PrescriptionViewDto(
                        rx.getId(),
                        rx.getMedicine().getMedicineId(),
                        rx.getMedicine().getMedicineName(),
                        rx.getDosage(),
                        rx.getFrequency(),
                        rx.getReminderTimes().stream()
                                .map(rt -> rt.getReminderTime().toString())
                                .toList(),
                        rx.getMedicine().getUnitDose()
                ))
                .toList();
    }

    public boolean prescriptionExists(Long patientId, Integer medicineId) {
        return prescriptionRepository.existsByPatientIdAndMedicine_MedicineId(patientId, medicineId);
    }

    public Optional<Prescription> findPrescription(Long id) {
        return prescriptionRepository.findById(id);
    }

    public void createPrescription(Patient patient, Medicine medicine, PrescriptionCreateDto dto,
                                  Long adminId, String adminUsername) {

        Prescription rx = new Prescription();
        rx.setPatient(patient);
        rx.setMedicine(medicine);
        rx.setDosage(dto.getDosage().trim());
        rx.setFrequency(dto.getFrequency().trim());

        if (!isBlank(dto.getStartDate())) {
            rx.setStartDate(LocalDate.parse(dto.getStartDate()));
        }
        if (!isBlank(dto.getEndDate())) {
            rx.setEndDate(LocalDate.parse(dto.getEndDate()));
        }

        List<String> times = dto.getScheduledTimes();
        if (times == null || times.isEmpty()) {
            times = dto.getReminderTimes();
        }
        if (times != null && !times.isEmpty()) {
            applyScheduledTimes(rx, times);
        }

        prescriptionRepository.save(rx);

        // Log the activity
        String patientName = patient.getFirstName() + " " + patient.getLastName();
        activityLogService.logPrescriptionCreated(
                patient.getId(),
                patientName,
                medicine.getMedicineName(),
                dto.getDosage().trim(),
                dto.getFrequency().trim(),
                adminId,
                adminUsername
        );
    }

    public void updatePrescription(Prescription rx, PrescriptionUpdateDto dto,
                                   Long adminId, String adminUsername) {
        rx.setDosage(dto.getDosage().trim());
        rx.setFrequency(dto.getFrequency().trim());

        if (!isBlank(dto.getStartDate())) {
            rx.setStartDate(LocalDate.parse(dto.getStartDate()));
        }
        if (!isBlank(dto.getEndDate())) {
            rx.setEndDate(LocalDate.parse(dto.getEndDate()));
        }

        List<String> times = dto.getScheduledTimes();
        if (times == null || times.isEmpty()) {
            times = dto.getReminderTimes();
        }
        if (times != null) {
            applyScheduledTimes(rx, times);
        }

        prescriptionRepository.save(rx);

        Patient patient = rx.getPatient();
        String patientName = patient.getFirstName() + " " + patient.getLastName();
        activityLogService.logPrescriptionUpdated(
                patient.getId(),
                patientName,
                rx.getMedicine().getMedicineName(),
                dto.getDosage().trim(),
                dto.getFrequency().trim(),
                adminId,
                adminUsername
        );
    }

    private void applyScheduledTimes(Prescription rx, List<String> scheduledTimes) {
        rx.getReminderTimes().clear();

        if (scheduledTimes == null || scheduledTimes.isEmpty()) {
            return;
        }

        for (String timeStr : scheduledTimes) {
            String trimmed = timeStr.trim();
            if (trimmed.isEmpty()) continue;

            PrescriptionReminderTime prt = new PrescriptionReminderTime();
            prt.setPrescription(rx);
            prt.setReminderTime(LocalTime.parse(trimmed));
            rx.getReminderTimes().add(prt);
        }
    }

    public boolean prescriptionIdExists(Long id) {
        return prescriptionRepository.existsById(id);
    }

    public void deletePrescription(Long id, Long adminId, String adminUsername) {
        // Get prescription details before deleting for logging
        Optional<Prescription> rxOpt = prescriptionRepository.findById(id);
        if (rxOpt.isPresent()) {
            Prescription rx = rxOpt.get();
            Patient patient = rx.getPatient();
            String patientName = patient.getFirstName() + " " + patient.getLastName();

            // Log the activity before deletion
            activityLogService.logPrescriptionDeleted(
                    patient.getId(),
                    patientName,
                    rx.getMedicine().getMedicineName(),
                    rx.getDosage(),
                    rx.getFrequency(),
                    adminId,
                    adminUsername
            );
        }

        prescriptionRepository.deleteById(id);
    }


    public List<MedicineViewDto> listMedicines() {
        return medicineRepository.findAll().stream()
                .map(m -> new MedicineViewDto(
                        m.getMedicineId(),
                        m.getMedicineName(),
                        m.getInstruction(),
                        m.getUnitDose()
                ))
                .toList();
    }

    public Optional<Medicine> findMedicineById(Integer medicineId) {
        return medicineRepository.findById(medicineId);
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

}
