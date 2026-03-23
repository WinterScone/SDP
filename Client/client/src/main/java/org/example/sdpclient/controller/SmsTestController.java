package org.example.sdpclient.controller;

import org.example.sdpclient.service.SmsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "twilio.enabled", havingValue = "true")
public class SmsTestController {

    private final SmsService smsService;

    public SmsTestController(SmsService smsService) {
        this.smsService = smsService;
    }

    @PostMapping("/sms")
    public Map<String, String> sendTestSmsPost(@RequestParam String to) {
        return send(to);
    }

    @GetMapping("/sms")
    public Map<String, String> sendTestSmsGet(@RequestParam String to) {
        return send(to);
    }

    private Map<String, String> send(String to) {
        try {
            String sid = smsService.sendSms(
                    to,
                    "Test SMS from SDP integration."
            );
            return Map.of("status", "success", "sid", sid);
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}