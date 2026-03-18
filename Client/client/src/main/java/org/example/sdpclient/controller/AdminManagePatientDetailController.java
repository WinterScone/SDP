package org.example.sdpclient.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern;
import org.example.sdpclient.dto.*;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.service.AdminManagePatientDetailService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminManagePatientDetailController {

    private final AdminManagePatientDetailService service;

    public AdminManagePatientDetailController(AdminManagePatientDetailService service) {
        this.service = service;
    }

    @GetMapping("/patients/search")
    public List<PatientViewDto> searchPatients(@RequestParam(required = false) String q, HttpServletRequest request) {
        Long adminId = getAdminIdFromCookie(request);
        boolean isRoot = isRootAdmin(request);
        return service.searchPatients(q, adminId, isRoot);
    }

    @GetMapping("/patients/{id}")
    public PatientViewDto getPatient(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = getAdminIdFromCookie(request);
        boolean isRoot = isRootAdmin(request);

        if (!service.canAdminAccessPatient(id, adminId, isRoot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this patient");
        }

        var patient = service.findPatient(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        var prescriptions = service.getPrescriptionViews(id);

        return new PatientViewDto(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth().toString(),
                patient.getEmail(),
                patient.getPhone(),
                prescriptions
        );
    }

    @GetMapping("/medicines")
    public List<MedicineViewDto> listMedicines() {
        return service.listMedicines();
    }

    @PostMapping("/patients/{id}/prescriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public void addPrescription(@PathVariable Long id, @RequestBody PrescriptionCreateDto dto, HttpServletRequest request) {
        Long adminId = getAdminIdFromCookie(request);
        boolean isRoot = isRootAdmin(request);

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

        String adminUsername = getCookieValue(request, "adminUsername");
        service.createPrescription(patient, medicine, dto, adminId, adminUsername);
    }

    @PutMapping("/prescriptions/{id}")
    public void updatePrescription(@PathVariable Long id, @RequestBody PrescriptionUpdateDto dto, HttpServletRequest request) {
        Long adminId = getAdminIdFromCookie(request);
        boolean isRoot = isRootAdmin(request);

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
        Long adminId = getAdminIdFromCookie(request);
        boolean isRoot = isRootAdmin(request);

        var rx = service.findPrescription(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found"));

        if (!service.canAdminAccessPatient(rx.getPatient().getId(), adminId, isRoot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this patient");
        }

        String adminUsername = getCookieValue(request, "adminUsername");
        service.deletePrescription(id, adminId, adminUsername);
    }

    private Long getAdminIdFromCookie(HttpServletRequest request) {
        String idStr = getCookieValue(request, "adminId");
        if (idStr == null || idStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin ID");
        }
    }

    private boolean isRootAdmin(HttpServletRequest request) {
        String rootStr = getCookieValue(request, "adminRoot");
        return "true".equalsIgnoreCase(rootStr);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }
}
