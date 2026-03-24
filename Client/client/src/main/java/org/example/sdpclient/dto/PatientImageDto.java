package org.example.sdpclient.dto;

public record PatientImageDto(
        Long patientId,
        String image,
        String contentType
) {}
