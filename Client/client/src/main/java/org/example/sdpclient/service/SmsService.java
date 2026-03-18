package org.example.sdpclient.service;


import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    public String sendMedicationReminder(String toPhoneNumber, String messageBody) {
        System.out.println("Sending FROM: " + twilioPhoneNumber);
        System.out.println("Sending TO: " + toPhoneNumber);

        Message message = Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(twilioPhoneNumber),
                messageBody
        ).create();

        return message.getSid();
    }
}