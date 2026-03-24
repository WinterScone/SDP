package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
public class SeedMedicine implements ApplicationRunner {

    private final MedicineRepository repository;

    public SeedMedicine(MedicineRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Remove orphaned medicines (and their FK dependents) whose type no
        // longer exists in the enum. Uses native queries to avoid JPA
        // deserialization errors on invalid enum values.
        List<Integer> validIds = Arrays.stream(MedicineType.values())
                .map(MedicineType::getId)
                .toList();
        repository.deleteOrphanReminderTimes(validIds);
        repository.deleteOrphanReminderLogs(validIds);
        repository.deleteOrphanPrescriptions(validIds);
        repository.deleteOrphanDispenserSlots(validIds);
        repository.deleteOrphanMedicines(validIds);

        // Upsert: create missing medicines, sync enum fields on existing ones
        for (MedicineType t : MedicineType.values()) {
            Medicine m = repository.findById(t.getId()).orElseGet(() -> {
                Medicine med = new Medicine();
                med.setQuantity(20);
                return med;
            });

            m.setMedicineId(t.getId());

            String name = t.getName();
            if (name == null || name.isBlank()) {
                name = t.name();
            }

            m.setMedicineName(name);
            m.setShape(t.getShape());
            m.setColour(t.getColour());
            m.setUnitDose(t.getDosage());

            // Preserve existing quantity; only fix invalid values
            if (m.getQuantity() < 0) {
                m.setQuantity(0);
            }

            repository.save(m);
        }
    }
}
