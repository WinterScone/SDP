package org.example.sdpclient.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.sdpclient.enums.MedicineType;

import java.util.List;

@Getter
@Setter
public class PrescriptionCreateDto {
    private MedicineType medicineId;
    private String dosage;
    private String frequency;
    private String startDate;
    private String endDate;
    private List<String> reminderTimes;
    private List<String> scheduledTimes;
}