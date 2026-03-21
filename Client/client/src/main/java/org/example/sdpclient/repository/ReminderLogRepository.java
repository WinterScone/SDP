package org.example.sdpclient.repository;

import org.example.sdpclient.entity.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    Optional<ReminderLog> findByPatientIdAndPrescriptionIdAndScheduledTime(
            Long patientId,
            Long prescriptionId,
            LocalDateTime scheduledTime
    );
}
