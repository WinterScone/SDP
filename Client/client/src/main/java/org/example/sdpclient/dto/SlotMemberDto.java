package org.example.sdpclient.dto;

public record SlotMemberDto(
        Long prescriptionId,
        String medicineName,
        String originalTime,
        String adjustedTime
) {}
