package org.example.sdpclient.controller;

import org.example.sdpclient.dto.IntakeLogRequest;
import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.service.IntakeHistoryService;
import org.example.sdpclient.service.PatientDetailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientDetailService patientDetailService;
    private final IntakeHistoryService intakeHistoryService;

    public PatientController(PatientDetailService patientDetailService,
                             IntakeHistoryService intakeHistoryService) {
        this.patientDetailService = patientDetailService;
        this.intakeHistoryService = intakeHistoryService;
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

    @PostMapping("/{patientId}/intake")
    public ResponseEntity<?> logIntake(@PathVariable Long patientId,
                                       @RequestBody IntakeLogRequest req) {
        var result = intakeHistoryService.logIntake(patientId, req);
        if (!Boolean.TRUE.equals(result.get("ok"))) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{patientId}/intake")
    public ResponseEntity<?> getHistory(@PathVariable Long patientId) {
        var history = intakeHistoryService.getHistory(patientId);
        return ResponseEntity.ok(Map.of("patientId", patientId, "history", history));
    }
}
