package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.enums.FrequencyType;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedPrescription {

    @Bean
    CommandLineRunner seed(PatientRepository patientRepo,
                           MedicineRepository medicineRepo,
                           PrescriptionRepository prescriptionRepo) {
        return args -> {
            var patientOptional = patientRepo.findByUsername("testPatient1");
            if (patientOptional.isEmpty()) return;
            var patient = patientOptional.get();

            addIfMissing(patient, MedicineType.VTM01, "1000", FrequencyType.TWICE_A_DAY, medicineRepo, prescriptionRepo);
            addIfMissing(patient, MedicineType.VTM02, "268", FrequencyType.ONCE_A_DAY, medicineRepo, prescriptionRepo);
            addIfMissing(patient, MedicineType.VTM03, "100", FrequencyType.FOUR_TIMES_A_DAY, medicineRepo, prescriptionRepo);
        };
    }

    private void addIfMissing(Patient patient, MedicineType medicineType, String dosage, FrequencyType frequency,
                              MedicineRepository medicineRepo, PrescriptionRepository prescriptionRepo) {
        if (prescriptionRepo.existsByPatientIdAndMedicine_MedicineId(patient.getId(), medicineType)) return;

        var medOptional = medicineRepo.findById(medicineType);
        if (medOptional.isEmpty()) return;

        Prescription p = new Prescription();
        p.setPatient(patient);
        p.setMedicine(medOptional.get());
        p.setDosage(dosage);
        p.setFrequency(frequency);
        prescriptionRepo.save(p);
    }
}
