package org.example.sdpclient.dto;

import java.util.List;

public record PatientViewDto(
        Long id,
        String firstName,
        String lastName,
        String dateOfBirth,
        String email,
        String phone,
        List<PrescriptionViewDto> prescriptions
) {}

