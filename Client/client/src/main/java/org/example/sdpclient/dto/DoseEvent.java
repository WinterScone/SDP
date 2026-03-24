package org.example.sdpclient.dto;

import java.time.LocalTime;

public record DoseEvent(
        Long prescriptionId,
        Integer medicineId,
        LocalTime targetTime,
        LocalTime earliestAllowed,
        LocalTime latestAllowed,
        int doseIndex,
        int totalDosesPerDay
) {}
