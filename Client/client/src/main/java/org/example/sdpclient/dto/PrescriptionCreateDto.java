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
    private String startDate;          // yyyy-MM-dd
    private String endDate;            // yyyy-MM-dd
    private List<String> reminderTimes; // ["08:00:00","20:00:00"]
}