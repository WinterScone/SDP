package org.example.sdpclient.dto;

public record MedicineViewDto(
        Integer medicineId,
        String medicineName,
        String instruction,
        Integer unitDose
) {}
