package org.example.sdpclient.controller;

import org.example.sdpclient.dto.IntakeLogRequest;
import org.example.sdpclient.service.IntakeHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/patients")
public class IntakeHistoryController {

    private final IntakeHistoryService intakeHistoryService;

    public IntakeHistoryController(IntakeHistoryService intakeHistoryService) {
        this.intakeHistoryService = intakeHistoryService;
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
