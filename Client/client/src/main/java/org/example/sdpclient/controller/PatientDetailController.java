package org.example.sdpclient.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.dto.PatientSummaryDto;
import org.example.sdpclient.dto.PatientViewDto;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.service.PatientDetailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public List<PatientSummaryDto> getAllPatients(HttpServletRequest request) {
        Long adminId = getAdminIdFromCookie(request);
        boolean isRoot = isRootAdmin(request);

        return service.getAllPatientsForAdmin(adminId, isRoot)
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
    public ResponseEntity<?> getPatientPrescriptions(@PathVariable Long patientId, HttpServletRequest request) {
        Long adminId = getAdminIdFromCookie(request);
        boolean isRoot = isRootAdmin(request);

        if (!service.patientExists(patientId)) {
            return ResponseEntity.status(400)
                    .body(Map.of(
                            "ok",
                            false,
                            "error",
                            "Patient not found")
                    );
        }

        // Check if admin has access to this patient
        var patients = service.getAllPatientsForAdmin(adminId, isRoot);
        boolean hasAccess = patients.stream().anyMatch(p -> p.getId().equals(patientId));

        if (!hasAccess) {
            return ResponseEntity.status(403)
                    .body(Map.of(
                            "ok",
                            false,
                            "error",
                            "Access denied to this patient")
                    );
        }

        var items = service.getPrescriptionItems(patientId);
        return ResponseEntity.ok(new PatientPrescriptionsResponse(patientId, items));
    }

    private Long getAdminIdFromCookie(HttpServletRequest request) {
        String idStr = getCookieValue(request, "adminId");
        if (idStr == null || idStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin ID");
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


