package org.example.sdpclient.controller;

import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.service.PatientDetailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/patient")
public class PatientDetailController {

    private final PatientDetailService service;

    public PatientDetailController(PatientDetailService service) {
        this.service = service;
    }

    @GetMapping("/{patientId}/prescriptions")
    public ResponseEntity<?> getPatientPrescriptions(@PathVariable Long patientId) {

        if (!service.patientExists(patientId)) {
            return ResponseEntity.status(400)
                    .body(Map.of(
                            "ok",
                            false,
                            "error",
                            "Patient not found")
                    );
        }

        var items = service.getPrescriptionItems(patientId);
        return ResponseEntity.ok(new PatientPrescriptionsResponse(patientId, items));
    }
}


