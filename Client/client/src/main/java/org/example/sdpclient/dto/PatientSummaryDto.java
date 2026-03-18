package org.example.sdpclient.dto;

public record PatientSummaryDto(
            Long id,
            String firstName,
            String lastName,
            String dateOfBirth,
            String email,
            String phone
) {}
