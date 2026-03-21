package org.example.sdpclient.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientRow(
        Long id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        LocalDateTime createdAt,
        Long linkedAdminId,
        String linkedAdminName
) {}
