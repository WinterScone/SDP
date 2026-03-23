package org.example.sdpclient.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispenseResultRequest {
    private Long patientId;
    private Long prescriptionId;
    private String scheduledTime; // example: 2026-03-17T08:00:00
    private String status;        // TAKEN / MISSED / FAILED / SKIPPED
    private String failureReason;
}