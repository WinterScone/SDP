package org.example.sdpclient.dto;

import java.util.List;

public record PrescriptionViewDto(
        Long id,
        Integer medicineId,
        String medicineName,
        String dosage,
        String frequency,
        List<String> scheduledTimes,
        Integer unitDose
) {}
