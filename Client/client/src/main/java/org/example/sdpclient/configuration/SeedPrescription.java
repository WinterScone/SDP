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
            var patientOptional = patientRepo.findByUsername("testPatient1");
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

            addIfMissing.accept(MedicineType.VTM01, new String[]{String.valueOf(1000), "Twice a day"});
            addIfMissing.accept(MedicineType.VTM02, new String[]{String.valueOf(268), "Once a day"});
            addIfMissing.accept(MedicineType.VTM03, new String[]{String.valueOf(100), "Three times a day"});
        };
    }
}


