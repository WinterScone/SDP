package org.example.sdpclient.controller;

import org.example.sdpclient.dto.AdminDto;
import org.example.sdpclient.dto.PatientRow;
import org.example.sdpclient.service.AdminListService;
import org.example.sdpclient.service.PatientAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminPatientController {

    private final PatientAdminService service;
    private final AdminListService adminListService;

    public AdminPatientController(PatientAdminService service, AdminListService adminListService) {
        this.service = service;
        this.adminListService = adminListService;
    }

    @GetMapping("/patients")
    public ResponseEntity<List<PatientRow>> getAllPatients() {
        return ResponseEntity.ok(service.getAllPatientsSafe());
    }

    @GetMapping("/admins")
    public ResponseEntity<List<AdminDto>> getAllAdmins() {
        return ResponseEntity.ok(adminListService.getAllAdmins());
    }

    @PutMapping("/patients/{patientId}/link-admin")
    public ResponseEntity<?> linkAdminToPatient(
            @PathVariable Long patientId,
            @RequestBody Map<String, Long> body) {

        Long adminId = body.get("adminId");
        if (adminId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "adminId is required"));
        }

        service.linkAdminToPatient(patientId, adminId);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
