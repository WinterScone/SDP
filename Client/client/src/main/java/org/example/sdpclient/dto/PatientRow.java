package org.example.sdpclient.dto;

public record PatientRow(
        Long id,
        String firstName,
        String lastName,
        String dateOfBirth,
        String email,
        String phone,
        String createdAt,
        Long linkedAdminId,
        String linkedAdminName
) {}
