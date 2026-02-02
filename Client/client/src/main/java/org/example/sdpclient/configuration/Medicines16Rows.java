package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class Medicines16Rows implements ApplicationRunner {

    private final MedicineRepository repository;

    public Medicines16Rows(MedicineRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (MedicineType medicineType : MedicineType.values()) {
            String number = medicineType.name().replace("MEDICINE_ID", "");
            String name = "MEDICINE_NAME" + number;

            repository.findById(medicineType).orElseGet(() ->
                    repository.save(new Medicine(medicineType, name, null, null, null, 0))
            );
        }
    }

}
