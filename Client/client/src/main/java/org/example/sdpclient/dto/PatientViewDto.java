package org.example.sdpclient.dto;

import java.time.LocalDate;
import java.util.List;

public record PatientViewDto(
        Long id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        List<PrescriptionViewDto> prescriptions
) {}
