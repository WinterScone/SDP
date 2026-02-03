package org.example.sdpclient.controller;

import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.dto.PatientSummaryDto;
import org.example.sdpclient.dto.PatientViewDto;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.service.PatientDetailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/patient")
public class PatientDetailController {

    private final PatientDetailService service;

    public PatientDetailController(PatientDetailService service) {
        this.service = service;
    }

    @GetMapping("/getAllPatients")
    public List<PatientSummaryDto> getAllPatients() {

        return service.getAllPatients()
                .stream()
                .map(p -> new PatientSummaryDto(
                        p.getId(),
                        p.getFirstName(),
                        p.getLastName(),
                        String.valueOf(p.getDateOfBirth()),
                        p.getEmail(),
                        p.getPhone()
                ))
                .toList();
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


