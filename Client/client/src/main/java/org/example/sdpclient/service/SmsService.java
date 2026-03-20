package org.example.sdpclient.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.example.sdpclient.configuration.TwilioConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final TwilioConfig twilioConfig;

    public SmsService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    public String sendSms(String to, String body) {
        if (twilioConfig.getAccountSid() == null || twilioConfig.getAccountSid().isBlank()) {
            log.warn("Twilio not configured — skipping SMS to {}", to);
            return "Twilio not configured";
        }

        try {
            String fromNumber = twilioConfig.getPhoneNumber();
            if (fromNumber == null || fromNumber.isBlank()) {
                log.warn("Twilio phone number not configured");
                return "Twilio phone number not configured";
            }
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromNumber),
                    body
            ).create();

            log.info("SMS sent to {} from {} — SID: {}", to, twilioConfig.getPhoneNumber(), message.getSid());
            return null;
        } catch (Exception e) {
            log.error("Failed to send SMS to {} from {}: {}", to, twilioConfig.getPhoneNumber(), e.getMessage());
            return "From: " + twilioConfig.getPhoneNumber() + " — " + e.getMessage();
        }
    }
}
