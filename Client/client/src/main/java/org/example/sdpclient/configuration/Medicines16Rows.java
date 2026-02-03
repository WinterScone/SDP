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

            // Use enum name as the DB id if your medicine_id column stores strings like "VITAMIN_C"
            String medicineId = t.name();

            // Avoid duplicates
            if (repository.existsById(MedicineType.valueOf(medicineId))) continue;

            Medicine m = new Medicine();
            m.setMedicineId(MedicineType.valueOf(medicineId));
            m.setMedicineName(t.getName());
            m.setShape(t.getShape());
            m.setColour(t.getColour());
            m.setDosagePerForm(t.getDosage());  // rename to match your entity field
            m.setQuantity(0);                   // or choose a default

            repository.save(m);
        }
    }

}
