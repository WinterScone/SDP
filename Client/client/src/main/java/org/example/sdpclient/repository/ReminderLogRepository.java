package org.example.sdpclient.repository;

import org.example.sdpclient.entity.ReminderLog;
import org.example.sdpclient.entity.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM reminder_log", nativeQuery = true)
    void deleteAllNative();

    @Transactional
    void deleteByPrescriptionId(Long prescriptionId);

    Optional<ReminderLog> findByPatientIdAndPrescriptionIdAndScheduledTime(
            Long patientId,
            Long prescriptionId,
            LocalDateTime scheduledTime
    );

    boolean existsByPatientIdAndPrescriptionIdAndScheduledTime(
            Long patientId,
            Long prescriptionId,
            LocalDateTime scheduledTime
    );

    // For DoseTrackingService: find NOTIFIED entries past their collection window
    List<ReminderLog> findByStatusAndScheduledTimeBefore(
            ReminderStatus status, LocalDateTime cutoff);

    // For IntakeHistoryService.getHistory(): all terminal entries for a patient
    List<ReminderLog> findByPatientIdAndStatusInOrderByScheduledTimeDesc(
            Long patientId, List<ReminderStatus> statuses);

    // For IntakeHistoryService.logIntake(): find today's NOTIFIED entry by patient + prescription
    Optional<ReminderLog> findFirstByPatientIdAndPrescription_Medicine_MedicineIdAndStatusAndScheduledTimeBetweenOrderByScheduledTimeAsc(
            Long patientId, Integer medicineId, ReminderStatus status,
            LocalDateTime from, LocalDateTime to);
}
