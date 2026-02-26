package org.example.sdpclient.controller;

import org.example.sdpclient.dto.IntakeLogRequest;
import org.example.sdpclient.service.IntakeHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/patient")
public class IntakeHistoryController {

    private final IntakeHistoryService service;

    public IntakeHistoryController(IntakeHistoryService service) {
        this.service = service;
    }

    @PostMapping("/{patientId}/intake")
    public ResponseEntity<?> logIntake(@PathVariable Long patientId,
                                       @RequestBody IntakeLogRequest req) {
        var result = service.logIntake(patientId, req);
        if (!Boolean.TRUE.equals(result.get("ok"))) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{patientId}/intake")
    public ResponseEntity<?> getHistory(@PathVariable Long patientId) {
        var history = service.getHistory(patientId);
        return ResponseEntity.ok(Map.of("patientId", patientId, "history", history));
    }
}
