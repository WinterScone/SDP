package org.example.sdpclient.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntakeLogRequest {
    private String medicineId;
    private String takenDate;
    private String takenTime;
    private String notes;
}
