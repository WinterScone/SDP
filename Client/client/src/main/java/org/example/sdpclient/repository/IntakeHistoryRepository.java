package org.example.sdpclient.repository;

import org.example.sdpclient.entity.IntakeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntakeHistoryRepository extends JpaRepository<IntakeHistory, Long> {

    List<IntakeHistory> findByPatientIdOrderByTakenDateDescTakenTimeDesc(Long patientId);
}
