package org.example.sdpclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PatientPrescriptionsResponse {
    private List<PrescriptionItem> prescriptions;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PrescriptionItem {
        private Long prescriptionId;
        private Integer medicineNumber;   // 1..13 for servo/slot mapping
        private String medicineCode;      // e.g. VTM01
        private String medicineName;      // e.g. Vitamin C
        private String dosage;
        private String scheduledTime;
    }
}