package org.example.sdpclient.repository;

import org.example.sdpclient.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientId(Long patientId);

    boolean existsByPatientIdAndMedicine_MedicineId(Long id, Integer medicineId);

    List<Prescription> findByPatientIdAndActiveTrue(Long patientId);

    List<Prescription> findByActiveTrue();

    @Modifying
    @Query(value = "DELETE FROM prescription_reminder_time", nativeQuery = true)
    void deleteAllReminderTimesNative();

    @Modifying
    @Query(value = "DELETE FROM prescription", nativeQuery = true)
    void deleteAllPrescriptionsNative();
}
