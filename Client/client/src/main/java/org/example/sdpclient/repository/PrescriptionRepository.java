package org.example.sdpclient.repository;

import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.enums.MedicineType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientId(Long patientId);
    List<Prescription> findByPatientIdAndActiveTrue(Long patientId);
    boolean existsByPatientIdAndMedicine_MedicineId(Long id, MedicineType medType);
}


