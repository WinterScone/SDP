package org.example.sdpclient.service;

import jakarta.transaction.Transactional;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicineService {
    private final MedicineRepository repo;

    public MedicineService(MedicineRepository repo) {
        this.repo = repo;
    }

    public List<Medicine> getAll() {
        return repo.findAll(Sort.by("medicineId"));
    }

    public boolean exists(MedicineType id) {
        return repo.existsById(id);
    }

    public void updateQuantity(MedicineType id, int quantity) {
        Medicine medicine = repo.findById(id).orElseThrow();
        medicine.setQuantity(quantity);
        repo.save(medicine);
    }


    @Transactional
    public void reduceQuantityByName(String medicineName, int quantityToReduce) {
        Medicine medicine = repo.findByMedicineName(medicineName)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medicineName));

        if (quantityToReduce <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0");
        }

        int current = medicine.getQuantity();
        if (current < quantityToReduce) {
            throw new IllegalArgumentException("Not enough stock. Current=" + current);
        }

        medicine.setQuantity(current - quantityToReduce);
        repo.save(medicine);
    }
}



