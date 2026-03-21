package org.example.sdpclient.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.sdpclient.enums.FrequencyType;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class PrescriptionUpdateDto{
    String dosage;
    String frequency;
    String startDate;
    String endDate;
    List<String> reminderTimes;
    List<String> scheduledTimes;
}