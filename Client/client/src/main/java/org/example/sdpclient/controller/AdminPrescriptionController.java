package org.example.sdpclient.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.sdpclient.dto.*;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.service.AdminManagePatientDetailService;
import org.example.sdpclient.util.CookieUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminPrescriptionController {

    private final AdminManagePatientDetailService service;

    public AdminPrescriptionController(AdminManagePatientDetailService service) {
        this.service = service;
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
                || AdminManagePatientDetailService.isBlank(dto.getFrequency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "medicineId, dosage and frequency are required"
            );
        }

        var patient = service.findPatient(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        MedicineType medId = dto.getMedicineId();
        var medicine = service.findMedicineById(medId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found"));

        if (service.prescriptionExists(id, medId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prescription already exists for this medicine"
            );
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
                || AdminManagePatientDetailService.isBlank(dto.getFrequency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dosage and frequency are required"
            );
        }

        var rx = service.findPrescription(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found"));

        if (!service.canAdminAccessPatient(rx.getPatient().getId(), adminId, isRoot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this patient");
        }

        service.updatePrescription(rx, dto);
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
