package org.example.sdpclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogDto {
    private Long id;
    private String activityType;
    private String description;
    private Long adminId;
    private String adminUsername;
    private Long patientId;
    private String patientName;
    private String medicineName;
    private String additionalDetails;
    private LocalDateTime timestamp;
}
