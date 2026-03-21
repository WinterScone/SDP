package org.example.sdpclient.service;

import org.example.sdpclient.dto.IntakeLogRequest;
import org.example.sdpclient.entity.IntakeHistory;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.IntakeHistoryRepository;
import org.example.sdpclient.repository.MedicineRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class IntakeHistoryService {

    private final IntakeHistoryRepository intakeHistoryRepo;
    private final PatientRepository patientRepo;
    private final MedicineRepository medicineRepo;

    public IntakeHistoryService(IntakeHistoryRepository intakeHistoryRepo,
                                PatientRepository patientRepo,
                                MedicineRepository medicineRepo) {
        this.intakeHistoryRepo = intakeHistoryRepo;
        this.patientRepo = patientRepo;
        this.medicineRepo = medicineRepo;
    }

    public Map<String, Object> logIntake(Long patientId, IntakeLogRequest req) {
        Patient patient = patientRepo.findById(patientId).orElse(null);
        if (patient == null) {
            return Map.of("ok", false, "error", "Patient not found");
        }

        MedicineType medicineType;
        try {
            medicineType = MedicineType.valueOf(req.getMedicineId());
        } catch (IllegalArgumentException e) {
            return Map.of("ok", false, "error", "Invalid medicine ID");
        }

        Medicine medicine = medicineRepo.findById(medicineType).orElse(null);
        if (medicine == null) {
            return Map.of("ok", false, "error", "Medicine not found");
        }

        LocalDate date;
        LocalTime time;
        try {
            date = (req.getTakenDate() != null && !req.getTakenDate().isBlank())
                    ? LocalDate.parse(req.getTakenDate())
                    : LocalDate.now();
            time = (req.getTakenTime() != null && !req.getTakenTime().isBlank())
                    ? LocalTime.parse(req.getTakenTime())
                    : LocalTime.now().withSecond(0).withNano(0);
        } catch (Exception e) {
            return Map.of("ok", false, "error", "Invalid date or time format");
        }

        IntakeHistory record = new IntakeHistory();
        record.setPatient(patient);
        record.setMedicine(medicine);
        record.setTakenDate(date);
        record.setTakenTime(time);
        record.setNotes(req.getNotes() != null && !req.getNotes().isBlank() ? req.getNotes().trim() : null);

        intakeHistoryRepo.save(record);
        return Map.of("ok", true);
    }

    public List<Map<String, Object>> getHistory(Long patientId) {
        return intakeHistoryRepo
                .findByPatientIdOrderByTakenDateDescTakenTimeDesc(patientId)
                .stream()
                .map(h -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", h.getId());
                    m.put("medicineId", h.getMedicine().getMedicineId().name());
                    m.put("medicineName", h.getMedicine().getMedicineName());
                    m.put("takenDate", h.getTakenDate().toString());
                    m.put("takenTime", h.getTakenTime().toString());
                    m.put("notes", h.getNotes() != null ? h.getNotes() : "");
                    return m;
                })
                .toList();
    }
}
