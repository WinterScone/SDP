package org.example.sdpclient.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispenseResultRequest {
    private Long patientId;
    private Long prescriptionId;
    private String scheduledTime;
    private String status;
    private String failureReason;
}
