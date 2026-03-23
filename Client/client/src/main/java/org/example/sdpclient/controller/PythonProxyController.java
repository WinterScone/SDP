package org.example.sdpclient.controller;

import org.example.sdpclient.dto.PythonVerifyRequest;
import org.example.sdpclient.service.PythonProxyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/python")
public class PythonProxyController {

    private final PythonProxyService service;

    public PythonProxyController(PythonProxyService service) {
        this.service = service;
    }

    /**
     * POST /api/python/verify
     *
     * Accepts { imageId, imageUrl, imageAccessToken } from the caller,
     * attaches a server-generated requestId, then forwards to the Python
     * service as:
     * {
     *   "request_id":          "<uuid>",
     *   "image_id":            <long>,
     *   "image_url":           "<url>",
     *   "image_access_token":  "<token>"
     * }
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, Object> body) {
        Object imageIdRaw = body.get("imageId");
        Object imageUrl = body.get("imageUrl");
        Object imageAccessToken = body.get("imageAccessToken");

        if (imageIdRaw == null || imageUrl == null || imageAccessToken == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "error", "imageId, imageUrl and imageAccessToken are required"));
        }

        long imageId;
        try {
            imageId = Long.parseLong(String.valueOf(imageIdRaw));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "error", "imageId must be a number"));
        }

        String requestId = UUID.randomUUID().toString();

        PythonVerifyRequest payload = new PythonVerifyRequest(
                requestId,
                imageId,
                String.valueOf(imageUrl),
                String.valueOf(imageAccessToken)
        );

        try {
            Map<?, ?> result = service.verify(payload);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(502)
                    .body(Map.of("ok", false, "error", e.getMessage()));
        }
    }
}
