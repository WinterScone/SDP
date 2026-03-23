package org.example.sdpclient.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sdpclient.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @deprecated Legacy endpoint aliases for backward compatibility with the remote/dispenser hardware.
 * These forward to the canonical endpoints. Remove once all clients have migrated.
 */
@Deprecated
@RestController
public class LegacyEndpointController {

    private final AdminAuthController adminAuthController;
    private final PatientAuthController patientAuthController;
    private final AdminPatientController adminPatientController;
    private final PatientController patientController;
    private final IntakeHistoryController intakeHistoryController;

    public LegacyEndpointController(AdminAuthController adminAuthController,
                                    PatientAuthController patientAuthController,
                                    AdminPatientController adminPatientController,
                                    PatientController patientController,
                                    IntakeHistoryController intakeHistoryController) {
        this.adminAuthController = adminAuthController;
        this.patientAuthController = patientAuthController;
        this.adminPatientController = adminPatientController;
        this.patientController = patientController;
        this.intakeHistoryController = intakeHistoryController;
    }

    // --- Admin auth aliases ---

    @Deprecated
    @PostMapping("/api/verify/login")
    public ResponseEntity<?> legacyAdminLogin(@RequestBody AdminLogin req, HttpServletResponse response) {
        return adminAuthController.login(req, response);
    }

    @Deprecated
    @PostMapping("/api/verify/logout")
    public ResponseEntity<?> legacyAdminLogout(HttpServletResponse response) {
        return adminAuthController.logout(response);
    }

    @Deprecated
    @GetMapping("/api/verify/me")
    public ResponseEntity<?> legacyAdminMe(HttpServletRequest request) {
        return adminAuthController.me(request);
    }

    @Deprecated
    @PostMapping("/api/verify/register")
    public ResponseEntity<?> legacyAdminRegister(HttpServletRequest request, @RequestBody AdminRegisterRequest req) {
        return adminAuthController.register(request, req);
    }

    // --- Patient auth aliases ---

    @Deprecated
    @PostMapping("/api/patient/login")
    public ResponseEntity<?> legacyPatientLogin(@RequestBody PatientLogin req, HttpServletResponse response) {
        return patientAuthController.login(req, response);
    }

    @Deprecated
    @PostMapping("/api/patient/signup")
    public ResponseEntity<?> legacyPatientSignup(@RequestBody PatientSignup req) {
        return patientAuthController.signup(req);
    }

    // --- Patient data aliases ---

    @Deprecated
    @GetMapping("/api/patient/getAllPatients")
    public ResponseEntity<?> legacyGetAllPatients(HttpServletRequest request) {
        return ResponseEntity.ok(adminPatientController.getAllPatients(request));
    }

    @Deprecated
    @GetMapping("/api/patient/{id}")
    public ResponseEntity<?> legacyGetPatient(@PathVariable Long id, HttpServletRequest request) {
        return ResponseEntity.ok(adminPatientController.getPatient(id, request));
    }

    @Deprecated
    @GetMapping("/api/patient/{id}/prescriptions")
    public ResponseEntity<?> legacyGetPatientPrescriptions(@PathVariable Long id) {
        return patientController.getPatientPrescriptions(id);
    }

    @Deprecated
    @PostMapping("/api/patient/{id}/intake")
    public ResponseEntity<?> legacyLogIntake(@PathVariable Long id, @RequestBody IntakeLogRequest req) {
        return intakeHistoryController.logIntake(id, req);
    }

    @Deprecated
    @GetMapping("/api/patient/{id}/intake")
    public ResponseEntity<?> legacyGetIntakeHistory(@PathVariable Long id) {
        return intakeHistoryController.getHistory(id);
    }
}
