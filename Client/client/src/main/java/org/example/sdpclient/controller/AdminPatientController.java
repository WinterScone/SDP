package org.example.sdpclient.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.example.sdpclient.dto.*;
import org.example.sdpclient.service.AdminListService;
import org.example.sdpclient.service.AdminManagePatientDetailService;
import org.example.sdpclient.service.DatabaseResetService;
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
    private final AdminListService adminListService;
    private final DatabaseResetService resetService;

    public AdminPatientController(PatientAdminService patientAdminService,
                                  PatientDetailService patientDetailService,
                                  AdminManagePatientDetailService adminManageService,
                                  PatientImageService patientImageService,
                                  AdminListService adminListService,
                                  DatabaseResetService resetService) {
        this.patientAdminService = patientAdminService;
        this.patientDetailService = patientDetailService;
        this.adminManageService = adminManageService;
        this.patientImageService = patientImageService;
        this.adminListService = adminListService;
        this.resetService = resetService;
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

    @GetMapping("/admins")
    public ResponseEntity<List<AdminDto>> getAllAdmins() {
        return ResponseEntity.ok(adminListService.getAllAdmins());
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

    @PostMapping("/reset-database")
    public ResponseEntity<?> resetDatabase(HttpServletRequest request) {
        // Only root admin can reset database
        if (!isRootAdmin(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only root admin can reset the database");
        }

        try {
            var stats = resetService.getResetStats();
            Long adminId = getAdminIdFromCookie(request);
            String adminUsername = getCookieValue(request, "adminUsername");

            resetService.resetToSeedData(adminId, adminUsername);

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "message", String.format("Deleted %d admins, %d patients, %d prescriptions. Seed data preserved.",
                            stats.adminsToDelete(),
                            stats.patientsToDelete(),
                            stats.prescriptionsToDelete())
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "ok", false,
                            "message", "Failed to reset database: " + e.getMessage()
                    ));
        }
    }

    private boolean isRootAdmin(HttpServletRequest request) {
        String rootStr = getCookieValue(request, "adminRoot");
        return "true".equalsIgnoreCase(rootStr);
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

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }
}
