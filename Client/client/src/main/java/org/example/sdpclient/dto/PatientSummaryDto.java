package org.example.sdpclient.dto;

import java.time.LocalDate;

public record PatientSummaryDto(
            Long id,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String email,
            String phone
) {}
