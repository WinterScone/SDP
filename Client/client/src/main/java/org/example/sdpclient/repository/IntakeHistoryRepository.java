package org.example.sdpclient.repository;

import org.example.sdpclient.entity.IntakeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface IntakeHistoryRepository extends JpaRepository<IntakeHistory, Long> {

    List<IntakeHistory> findByPatientIdOrderByTakenDateDescTakenTimeDesc(Long patientId);

    boolean existsByPatient_IdAndMedicine_MedicineIdAndTakenDateAndTakenTimeBetween(
            Long patientId, Integer medicineId, LocalDate date, LocalTime start, LocalTime end);
}
