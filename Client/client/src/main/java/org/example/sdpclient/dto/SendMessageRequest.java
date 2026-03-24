package org.example.sdpclient.dto;

import org.example.sdpclient.service.AdminMessagingService.RecipientEntry;

import java.util.List;

public class SendMessageRequest {
    private List<RecipientEntry> recipients;
    private String message;

    public List<RecipientEntry> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<RecipientEntry> recipients) {
        this.recipients = recipients;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
