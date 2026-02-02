package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Prescription;
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
            var patientOptional = patientRepo.findByUsername("patient1");
            if (patientOptional.isEmpty()) return;
            var patient = patientOptional.get();

            java.util.function.BiConsumer<MedicineType, String[]> addIfMissing = (medicineType, df) -> {
                if (prescriptionRepo.existsByPatientIdAndMedicine_MedicineId(patient.getId(), medicineType)) return;

                var medOptional = medicineRepo.findById(medicineType);
                if (medOptional.isEmpty()) return;

                Prescription p = new Prescription();
                p.setPatient(patient);
                p.setMedicine(medOptional.get());
                p.setDosage(df[0]);
                p.setFrequency(df[1]);
                prescriptionRepo.save(p);
            };

            addIfMissing.accept(MedicineType.MEDICINE_ID1, new String[]{"500mg", "Twice a day"});
            addIfMissing.accept(MedicineType.MEDICINE_ID2, new String[]{"10mg", "Once a day"});
            addIfMissing.accept(MedicineType.MEDICINE_ID3, new String[]{"20mg", "Three times a day"});
        };
    }
}


