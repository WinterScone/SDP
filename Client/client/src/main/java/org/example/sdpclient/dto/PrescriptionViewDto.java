package org.example.sdpclient.dto;


import org.example.sdpclient.enums.MedicineType;

import java.util.List;

public record PrescriptionViewDto(
        Long id,
        MedicineType medicineId,
        String medicineName,
        String dosage,
        String frequency,
        List<String> scheduledTimes,
        Integer unitDose
) {}
