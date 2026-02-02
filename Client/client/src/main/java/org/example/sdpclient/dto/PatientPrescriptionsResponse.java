package org.example.sdpclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatientPrescriptionsResponse {

    private Long patientId;
    private List<PrescriptionItem> prescriptions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrescriptionItem {
        private String medicineId;
        private String medicineName;
        private String dosage;
        private String frequency;
    }
}
