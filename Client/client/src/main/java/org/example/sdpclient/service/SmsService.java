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

    public boolean sendSms(String to, String body) {
        if (twilioConfig.getAccountSid() == null || twilioConfig.getAccountSid().isBlank()) {
            log.warn("Twilio not configured — skipping SMS to {}", to);
            return false;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(twilioConfig.getPhoneNumber()),
                    body
            ).create();

            log.info("SMS sent to {} — SID: {}", to, message.getSid());
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", to, e.getMessage());
            return false;
        }
    }
}
