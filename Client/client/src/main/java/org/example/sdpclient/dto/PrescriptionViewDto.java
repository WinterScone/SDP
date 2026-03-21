package org.example.sdpclient.dto;


import org.example.sdpclient.enums.MedicineType;

public record PrescriptionViewDto(
        Long id,
        MedicineType medicineId,
        String medicineName,
        String dosage,
        String frequency
) {}
