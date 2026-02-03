package org.example.sdpclient.controller;

import org.example.sdpclient.dto.PatientRow;
import org.example.sdpclient.service.PatientAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminPatientController {

    private final PatientAdminService service;

    public AdminPatientController(PatientAdminService service) {
        this.service = service;
    }

    @GetMapping("/patients")
    public ResponseEntity<List<PatientRow>> getAllPatients() {
        return ResponseEntity.ok(service.getAllPatientsSafe());
    }
}
