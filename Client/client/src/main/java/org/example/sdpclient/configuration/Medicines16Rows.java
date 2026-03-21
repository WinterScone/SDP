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
        for (MedicineType t : MedicineType.values()) {

            // avoid duplicates (since id is already the enum)
            if (repository.existsById(t)) continue;

            Medicine m = new Medicine();
            m.setMedicineId(t);

            String name = t.getName();
            if (name == null || name.isBlank()) name = t.name();  // fallback

            m.setMedicineName(name);
            m.setShape(t.getShape());
            m.setColour(t.getColour());
            m.setDosagePerForm(t.getDosage());
            m.setQuantity(0);

            repository.save(m);
        }
    }


}
