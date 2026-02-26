package org.example.sdpclient.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.example.sdpclient.dto.AdminDto;
import org.example.sdpclient.dto.PatientRow;
import org.example.sdpclient.service.AdminListService;
import org.example.sdpclient.service.DatabaseResetService;
import org.example.sdpclient.service.PatientAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminPatientController {

    private final PatientAdminService service;
    private final AdminListService adminListService;
    private final DatabaseResetService resetService;

    public AdminPatientController(PatientAdminService service,
                                  AdminListService adminListService,
                                  DatabaseResetService resetService) {
        this.service = service;
        this.adminListService = adminListService;
        this.resetService = resetService;
    }

    @GetMapping("/patients")
    public ResponseEntity<List<PatientRow>> getAllPatients(HttpServletRequest request) {
        // This endpoint is for root admin to assign patients to admins
        if (!isRootAdmin(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only root admin can access this endpoint");
        }
        return ResponseEntity.ok(service.getAllPatientsSafe());
    }

    @GetMapping("/admins")
    public ResponseEntity<List<AdminDto>> getAllAdmins() {
        return ResponseEntity.ok(adminListService.getAllAdmins());
    }

    @PutMapping("/patients/{patientId}/link-admin")
    public ResponseEntity<?> linkAdminToPatient(
            @PathVariable Long patientId,
            @RequestBody Map<String, Long> body,
            HttpServletRequest request) {

        // Only root admin can link patients to admins
        if (!isRootAdmin(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only root admin can link patients to admins");
        }

        Long adminId = body.get("adminId");
        if (adminId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "adminId is required"));
        }

        service.linkAdminToPatient(patientId, adminId);
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
            resetService.resetToSeedData();

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

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }
}
