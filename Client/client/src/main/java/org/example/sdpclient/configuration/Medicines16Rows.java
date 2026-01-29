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
        for (MedicineType type : MedicineType.values()) {
            String number = type.name().replace("MEDICINE_ID", ""); // "1".."16"
            String name = "MEDICINE_NAME" + number;

            repository.findById(type).orElseGet(() ->
                    repository.save(new Medicine(type, name, null, null, null, 0))
            );
        }
    }

}
