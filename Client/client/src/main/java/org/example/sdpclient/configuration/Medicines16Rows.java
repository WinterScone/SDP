package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class Medicines16Rows implements ApplicationRunner {

    private final MedicineRepository repository;

    public Medicines16Rows(MedicineRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<MedicineType> validTypes = EnumSet.allOf(MedicineType.class);

        List<Medicine> existing = repository.findAll();
        for (Medicine medicine : existing) {
            if (!validTypes.contains(medicine.getMedicineId())) {
                repository.delete(medicine);
            }
        }

        for (MedicineType t : MedicineType.values()) {
            Medicine m = repository.findById(t).orElseGet(Medicine::new);

            m.setMedicineId(t);

            String name = t.getName();
            if (name == null || name.isBlank()) {
                name = t.name();
            }

            m.setMedicineName(name);
            m.setShape(t.getShape());
            m.setColour(t.getColour());
            m.setDosagePerForm(t.getDosage());

            if (m.getQuantity() < 0) {
                m.setQuantity(0);
            }

            repository.save(m);
        }
    }
}