package org.example.sdpclient.repository;

import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, MedicineType> {
    Optional<Medicine> findByMedicineName(String medicineName);
}
