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

        // Extended fields used by machine/dispenser endpoints
        private Long prescriptionId;
        private Integer medicineNumber;
        private String medicineCode;
        private String scheduledTime;

        // Constructor for existing usage (admin/patient views)
        public PrescriptionItem(String medicineId, String medicineName, String dosage, String frequency) {
            this.medicineId = medicineId;
            this.medicineName = medicineName;
            this.dosage = dosage;
            this.frequency = frequency;
        }

        // Constructor for machine/dispenser usage
        public PrescriptionItem(Long prescriptionId, Integer medicineNumber, String medicineCode,
                                String medicineName, String dosage, String frequency, String scheduledTime) {
            this.prescriptionId = prescriptionId;
            this.medicineId = medicineCode;
            this.medicineNumber = medicineNumber;
            this.medicineCode = medicineCode;
            this.medicineName = medicineName;
            this.dosage = dosage;
            this.frequency = frequency;
            this.scheduledTime = scheduledTime;
        }
    }
}
