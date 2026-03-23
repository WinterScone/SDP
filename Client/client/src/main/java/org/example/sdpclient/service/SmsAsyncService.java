package org.example.sdpclient.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SmsAsyncService {

    private final SmsService smsService;

    public SmsAsyncService(SmsService smsService) {
        this.smsService = smsService;
    }

    @Async
    public void sendSms(String to, String body) {
        smsService.sendSms(to, body);
    }
}
