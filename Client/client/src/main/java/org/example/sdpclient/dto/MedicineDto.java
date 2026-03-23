package org.example.sdpclient.dto;

public record MedicineDto(
        int id,
        String medicineId,
        String name,
        String shape,
        String colour,
        Integer dosage,
        int quantity
) {}
