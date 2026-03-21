package org.example.sdpclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PrescriptionUpdateDto {
    private String dosage;
    private String frequency;

    // old fields still used by controller/service
    private String startDate;
    private String endDate;
    private List<String> reminderTimes;

    // new field from your updated UI
    private List<String> scheduledTimes;
}