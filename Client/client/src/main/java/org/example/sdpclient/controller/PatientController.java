package org.example.sdpclient.controller;

import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.service.PatientDetailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientDetailService patientDetailService;

    public PatientController(PatientDetailService patientDetailService) {
        this.patientDetailService = patientDetailService;
    }

    @GetMapping("/{patientId}/prescriptions")
    public ResponseEntity<?> getPatientPrescriptions(@PathVariable Long patientId) {
        if (!patientDetailService.patientExists(patientId)) {
            return ResponseEntity.status(400)
                    .body(Map.of(
                            "ok",
                            false,
                            "error",
                            "Patient not found")
                    );
        }

        var items = patientDetailService.getPrescriptionItems(patientId);
        return ResponseEntity.ok(new PatientPrescriptionsResponse(patientId, items));
    }

    @GetMapping("/{patientId}/collectable")
    public ResponseEntity<?> getCollectableMedications(@PathVariable Long patientId) {
        if (!patientDetailService.patientExists(patientId)) {
            return ResponseEntity.status(400)
                    .body(Map.of(
                            "ok",
                            false,
                            "error",
                            "Patient not found")
                    );
        }

        var items = patientDetailService.getCollectableItems(patientId);
        return ResponseEntity.ok(new PatientPrescriptionsResponse(patientId, items));
    }
}
