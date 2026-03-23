package org.example.sdpclient.dto;

import org.example.sdpclient.enums.MedicineType;

public record MedicineViewDto(
        MedicineType medicineId,
        String medicineName
) {}