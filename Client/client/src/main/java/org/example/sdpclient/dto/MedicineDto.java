package org.example.sdpclient.dto;

public record MedicineDto(
        Integer medicineId,
        String name,
        String shape,
        String colour,
        Integer dosage,
        int quantity
) {}
