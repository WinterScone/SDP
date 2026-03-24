package org.example.sdpclient.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.sdpclient.dto.*;
import org.example.sdpclient.service.AdminManagePatientDetailService;
import org.example.sdpclient.service.CollectionTimeClusteringService;
import org.example.sdpclient.util.CookieUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminPrescriptionController {

    private final AdminManagePatientDetailService service;
    private final CollectionTimeClusteringService clusteringService;

    public AdminPrescriptionController(AdminManagePatientDetailService service,
                                       CollectionTimeClusteringService clusteringService) {
        this.service = service;
        this.clusteringService = clusteringService;
    }

    @GetMapping("/medicines")
    public List<MedicineViewDto> listMedicines() {
        return service.listMedicines();
    }

    @PostMapping("/patients/{id}/prescriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public void addPrescription(@PathVariable Long id, @RequestBody PrescriptionCreateDto dto, HttpServletRequest request) {
        Long adminId = CookieUtils.getAdminIdFromCookie(request);
        boolean isRoot = CookieUtils.isRootAdmin(request);

        if (!service.canAdminAccessPatient(id, adminId, isRoot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this patient");
        }

        if (dto == null || dto.getMedicineId() == null
                || AdminManagePatientDetailService.isBlank(dto.getDosage())
                || dto.getFrequency() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "medicineId, dosage and frequency are required");
        }

        if (dto.getFrequency() < 1 || dto.getFrequency() > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "frequency must be between 1 and 4");
        }

        var patient = service.findPatient(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        Integer medId = dto.getMedicineId();
        var medicine = service.findMedicineById(medId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found"));

        try {
            int dosage = Integer.parseInt(dto.getDosage().trim());
            int unitDose = medicine.getUnitDose();
            if (dosage % unitDose != 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Dosage must be a multiple of " + unitDose + "mg (dosage per unit)");
            }
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dosage must be a valid number");
        }

        if (service.prescriptionExists(id, medId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prescription already exists for this medicine");
        }

        // Resolve scheduled times: prefer scheduledTimes, fall back to reminderTimes
        List<String> times = dto.getScheduledTimes();
        if (times == null || times.isEmpty()) {
            times = dto.getReminderTimes();
        }
        if (times == null || times.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one scheduled time is required");
        }
        dto.setScheduledTimes(times);

        // Default start/end dates if not provided
        if (AdminManagePatientDetailService.isBlank(dto.getStartDate())) {
            dto.setStartDate(LocalDate.now().toString());
        }
        if (AdminManagePatientDetailService.isBlank(dto.getEndDate())) {
            dto.setEndDate(LocalDate.now().plusYears(1).toString());
        }

        String adminUsername = CookieUtils.getCookieValue(request, "adminUsername");
        service.createPrescription(patient, medicine, dto, adminId, adminUsername);
    }

    @PutMapping("/prescriptions/{id}")
    public void updatePrescription(@PathVariable Long id, @RequestBody PrescriptionUpdateDto dto, HttpServletRequest request) {
        Long adminId = CookieUtils.getAdminIdFromCookie(request);
        boolean isRoot = CookieUtils.isRootAdmin(request);

        if (dto == null
                || AdminManagePatientDetailService.isBlank(dto.getDosage())
                || dto.getFrequency() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dosage and frequency are required");
        }

        if (dto.getFrequency() < 1 || dto.getFrequency() > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "frequency must be between 1 and 4");
        }

        var rx = service.findPrescription(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found"));

        try {
            int dosage = Integer.parseInt(dto.getDosage().trim());
            int unitDose = rx.getMedicine().getUnitDose();
            if (dosage % unitDose != 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Dosage must be a multiple of " + unitDose + "mg (dosage per unit)");
            }
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dosage must be a valid number");
        }

        if (!service.canAdminAccessPatient(rx.getPatient().getId(), adminId, isRoot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this patient");
        }

        String adminUsername = CookieUtils.getCookieValue(request, "adminUsername");
        service.updatePrescription(rx, dto, adminId, adminUsername);
    }

    @GetMapping("/patients/{id}/prescriptions/clustering-preview")
    public ClusteringResultDto previewClustering(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = CookieUtils.getAdminIdFromCookie(request);
        boolean isRoot = CookieUtils.isRootAdmin(request);

        if (!service.canAdminAccessPatient(id, adminId, isRoot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this patient");
        }

        service.findPatient(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        return clusteringService.computeClusteredTimes(id);
    }

    @DeleteMapping("/prescriptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePrescription(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = CookieUtils.getAdminIdFromCookie(request);
        boolean isRoot = CookieUtils.isRootAdmin(request);

        var rx = service.findPrescription(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found"));

        if (!service.canAdminAccessPatient(rx.getPatient().getId(), adminId, isRoot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this patient");
        }

        String adminUsername = CookieUtils.getCookieValue(request, "adminUsername");
        service.deletePrescription(id, adminId, adminUsername);
    }
}
