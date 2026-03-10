package org.example.sdpclient.controller;

import org.example.sdpclient.service.SmsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/sms")
public class SmsTestController {

    private final SmsService smsService;

    public SmsTestController(SmsService smsService) {
        this.smsService = smsService;
    }

    @PostMapping("/test")
    public ResponseEntity<?> sendTestSms(@RequestBody Map<String, String> req) {
        String to = req.get("to");
        String message = req.get("message");

        if (to == null || to.isBlank() || message == null || message.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "error", "Both 'to' and 'message' are required"));
        }

        String error = smsService.sendSms(to, message);

        if (error == null) {
            return ResponseEntity.ok(Map.of("ok", true, "message", "SMS sent to " + to));
        } else {
            return ResponseEntity.status(500)
                    .body(Map.of("ok", false, "error", error));
        }
    }
}
