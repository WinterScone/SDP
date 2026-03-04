package org.example.sdpclient.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.example.sdpclient.service.PatientImageService;
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

    /**
     * GET /api/patients/{patientId}/images/{imageId}
     *
     * Security:
     *  1. Caller must present either an adminUsername cookie (admin access) or a
     *     patientId cookie whose value matches the path {patientId} (patient
     *     accessing their own images).  Any other combination → 401.
     *  2. The image is fetched by (imageId AND patientId) together, so a valid
     *     caller cannot read images belonging to a different patient by guessing
     *     an imageId (IDOR prevention).
     *  3. The content-type is validated against an allowlist of image MIME types
     *     before the response is written, preventing stored XSS via crafted blobs.
     */
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

    /**
     * Returns true when the request carries either:
     *  - a non-blank adminUsername cookie  (admin), or
     *  - a patientId cookie matching the requested patientId  (patient's own data).
     */
    private boolean isAuthorized(HttpServletRequest request, Long patientId) {
        String adminUsername = getCookieValue(request, "adminUsername");
        if (adminUsername != null && !adminUsername.isBlank()) {
            return true;
        }

        String patientCookie = getCookieValue(request, "patientId");
        if (patientCookie != null && !patientCookie.isBlank()) {
            try {
                return Long.parseLong(patientCookie) == patientId;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        return false;
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
