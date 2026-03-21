package org.example.sdpclient.controller;

import org.example.sdpclient.service.SmsService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/sms")
public class SmsTestController {

    private final SmsService smsService;

    public SmsTestController(SmsService smsService) {
        this.smsService = smsService;
    }

    @PostMapping("/test")
    public String sendTestSms(@RequestParam String phone) {
        String cleanPhone = phone.trim();

        if (!cleanPhone.startsWith("+")) {
            cleanPhone = "+" + cleanPhone;
        }

        System.out.println("Normalized phone: " + cleanPhone);

        String sid = smsService.sendMedicationReminder(
                cleanPhone,
                "Medication reminder: it's time to take your scheduled dose."
        );

        return "SMS sent successfully. SID: " + sid;
    }
}