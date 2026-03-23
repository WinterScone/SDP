package org.example.sdpclient.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.service.PatientDetailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientDetailService patientDetailService;

    public PatientController(PatientDetailService patientDetailService) {
        this.patientDetailService = patientDetailService;
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<?> getPatientById(@PathVariable Long patientId) {
        var patientOpt = patientDetailService.getPatientById(patientId);

        if (patientOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "ok", false,
                    "error", "Patient not found"
            ));
        }

        Patient p = patientOpt.get();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", p.getId());
        response.put("username", p.getUsername());
        response.put("firstName", p.getFirstName());
        response.put("lastName", p.getLastName());
        response.put("dateOfBirth", String.valueOf(p.getDateOfBirth()));
        response.put("email", p.getEmail());
        response.put("phone", p.getPhone());
        response.put("linkedAdminId", p.getLinkedAdmin() != null ? p.getLinkedAdmin().getId() : null);
        response.put("linkedAdminName", p.getLinkedAdmin() != null ? p.getLinkedAdmin().getUsername() : null);

        return ResponseEntity.ok(response);
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
