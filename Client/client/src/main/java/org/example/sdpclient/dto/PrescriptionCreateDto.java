package org.example.sdpclient.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.sdpclient.enums.MedicineType;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PrescriptionCreateDto {
    MedicineType medicineId;
    String dosage;
    String frequency;
    String startDate;
    String endDate;
    List<String> reminderTimes;
    List<String> scheduledTimes;
}
