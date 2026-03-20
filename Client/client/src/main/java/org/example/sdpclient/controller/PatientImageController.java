package org.example.sdpclient.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.sdpclient.service.PatientImageService;
import org.example.sdpclient.util.CookieUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/patients")
public class PatientImageController {

    private final PatientImageService service;

    public PatientImageController(PatientImageService service) {
        this.service = service;
    }

    @GetMapping("/{patientId}/images/{imageId}")
    public ResponseEntity<?> getImage(
            @PathVariable Long patientId,
            @PathVariable Long imageId,
            HttpServletRequest request) {

        if (!isAuthorized(request, patientId)) {
            return ResponseEntity.status(401)
                    .body(Map.of("ok", false, "message", "Unauthorized"));
        }

        if (!service.patientExists(patientId)) {
            return ResponseEntity.status(404)
                    .body(Map.of("ok", false, "message", "Patient not found"));
        }

        var imageOpt = service.findImage(patientId, imageId);
        if (imageOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("ok", false, "message", "Image not found"));
        }

        var image = imageOpt.get();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .body(image.getData());
    }

    private boolean isAuthorized(HttpServletRequest request, Long patientId) {
        String adminUsername = CookieUtils.getCookieValue(request, "adminUsername");
        if (adminUsername != null && !adminUsername.isBlank()) {
            return true;
        }

        String patientCookie = CookieUtils.getCookieValue(request, "patientId");
        if (patientCookie != null && !patientCookie.isBlank()) {
            try {
                return Long.parseLong(patientCookie) == patientId;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        return false;
    }
}
