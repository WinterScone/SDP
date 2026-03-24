package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.entity.PrescriptionReminderTime;
import org.example.sdpclient.enums.FrequencyType;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.example.sdpclient.service.CollectionTimeClusteringService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

@Configuration
public class SeedPrescription {

    @Bean
    CommandLineRunner seed(PatientRepository patientRepo,
                           MedicineRepository medicineRepo,
                           PrescriptionRepository prescriptionRepo,
                           CollectionTimeClusteringService clusteringService) {
        return args -> {
            var patientOptional = patientRepo.findByUsername("testPatient1");
            if (patientOptional.isEmpty()) return;
            var patient = patientOptional.get();

            java.util.function.BiConsumer<Integer, Object[]> addIfMissing = (medicineId, df) -> {
                if (prescriptionRepo.existsByPatientIdAndMedicine_MedicineId(patient.getId(), medicineId)) return;

                var medOptional = medicineRepo.findById(medicineId);
                if (medOptional.isEmpty()) return;

                Prescription p = new Prescription();
                p.setPatient(patient);
                p.setMedicine(medOptional.get());
                p.setDosage((String) df[0]);
                FrequencyType freq = (FrequencyType) df[1];
                p.setFrequency(freq.getValue());

                // Add default reminder times from FrequencyType
                for (Integer hour : freq.getDefaultHours()) {
                    PrescriptionReminderTime prt = new PrescriptionReminderTime();
                    prt.setPrescription(p);
                    prt.setReminderTime(LocalTime.of(hour, 0));
                    p.getReminderTimes().add(prt);
                }

                prescriptionRepo.save(p);
            };

            addIfMissing.accept(MedicineType.VTM01.getId(), new Object[]{String.valueOf(1000), FrequencyType.TWICE_A_DAY});
            addIfMissing.accept(MedicineType.VTM02.getId(), new Object[]{String.valueOf(268), FrequencyType.ONCE_A_DAY});
            addIfMissing.accept(MedicineType.VTM03.getId(), new Object[]{String.valueOf(100), FrequencyType.FOUR_TIMES_A_DAY});

            // Apply clustering after all seeds are created
            clusteringService.applyClusteredTimes(patient.getId());
        };
    }
}
