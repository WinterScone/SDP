package org.example.sdpclient.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SmsAsyncService {

    private static final Logger log = LoggerFactory.getLogger(SmsAsyncService.class);

    private final SmsService smsService;

    public SmsAsyncService(SmsService smsService) {
        this.smsService = smsService;
    }

    @Async
    public void sendSmsAsync(String to, String body) {
        String error = smsService.sendSms(to, body);
        if (error != null) {
            log.warn("Async SMS to {} failed: {}", to, error);
        }
    }
}
