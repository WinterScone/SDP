package org.example.sdpclient.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.sdpclient.dto.*;
import org.example.sdpclient.service.AdminManagePatientDetailService;
import org.example.sdpclient.service.PatientAdminService;
import org.example.sdpclient.service.PatientDetailService;
import org.example.sdpclient.service.PatientImageService;
import org.example.sdpclient.util.CookieUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/patients")
public class AdminPatientController {

    private final PatientAdminService patientAdminService;
    private final PatientDetailService patientDetailService;
    private final AdminManagePatientDetailService adminManageService;
    private final PatientImageService patientImageService;

    public AdminPatientController(PatientAdminService patientAdminService,
                                  PatientDetailService patientDetailService,
                                  AdminManagePatientDetailService adminManageService,
                                  PatientImageService patientImageService) {
        this.patientAdminService = patientAdminService;
        this.patientDetailService = patientDetailService;
        this.adminManageService = adminManageService;
        this.patientImageService = patientImageService;
    }

    @GetMapping
    public List<PatientSummaryDto> getAllPatients(HttpServletRequest request) {
        Long adminId = CookieUtils.getAdminIdFromCookie(request);
        boolean isRoot = CookieUtils.isRootAdmin(request);

        return patientDetailService.getAllPatientsForAdmin(adminId, isRoot)
                .stream()
                .map(p -> new PatientSummaryDto(
                        p.getId(),
                        p.getFirstName(),
                        p.getLastName(),
                        p.getDateOfBirth(),
                        p.getEmail(),
                        p.getPhone(),
                        p.isSmsConsent(),
                        p.isFaceActive(),
                        p.getLinkedAdmin() != null ? p.getLinkedAdmin().getUsername() : null
                ))
                .toList();
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<PatientRow>> getPatientAssignments(HttpServletRequest request) {
        if (!CookieUtils.isRootAdmin(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only root admin can access this endpoint");
        }
        return ResponseEntity.ok(patientAdminService.getAllPatientsSafe());
    }

    @GetMapping("/search")
    public List<PatientViewDto> searchPatients(@RequestParam(required = false) String q, HttpServletRequest request) {
        Long adminId = CookieUtils.getAdminIdFromCookie(request);
        boolean isRoot = CookieUtils.isRootAdmin(request);
        return adminManageService.searchPatients(q, adminId, isRoot);
    }

    @GetMapping("/{id}")
    public PatientViewDto getPatient(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = CookieUtils.getAdminIdFromCookie(request);
        boolean isRoot = CookieUtils.isRootAdmin(request);

        if (!adminManageService.canAdminAccessPatient(id, adminId, isRoot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this patient");
        }

        var patient = adminManageService.findPatient(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        var prescriptions = adminManageService.getPrescriptionViews(id);

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
                prescriptions
        );
    }

    @GetMapping("/images")
    public ResponseEntity<List<PatientImageDto>> getAllPatientImages(HttpServletRequest request) {
        if (!CookieUtils.isRootAdmin(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only root admin can access this endpoint");
        }
        return ResponseEntity.ok(patientImageService.getAllPatientImages());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePatient(
            @PathVariable Long id,
            @RequestBody PatientUpdateDto dto,
            HttpServletRequest request) {

        if (!CookieUtils.isRootAdmin(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only root admin can edit patient details");
        }

        try {
            Long adminId = CookieUtils.getAdminIdFromCookie(request);
            String adminUsername = CookieUtils.getCookieValue(request, "adminUsername");
            PatientViewDto updated = adminManageService.updatePatientDetails(id, dto, adminId, adminUsername);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("Patient not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
        }
    }

    @PutMapping("/{patientId}/link-admin")
    public ResponseEntity<?> linkAdminToPatient(
            @PathVariable Long patientId,
            @RequestBody Map<String, Long> body,
            HttpServletRequest request) {

        if (!CookieUtils.isRootAdmin(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only root admin can link patients to admins");
        }

        Long adminId = body.get("adminId");
        if (adminId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "adminId is required"));
        }

        Long assignerAdminId = CookieUtils.getAdminIdFromCookie(request);
        String assignerAdminUsername = CookieUtils.getCookieValue(request, "adminUsername");

        patientAdminService.linkAdminToPatient(patientId, adminId, assignerAdminId, assignerAdminUsername);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
