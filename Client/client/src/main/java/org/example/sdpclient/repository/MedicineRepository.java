package org.example.sdpclient.repository;

import org.example.sdpclient.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Integer> {
    Optional<Medicine> findByMedicineName(String medicineName);

    @Modifying
    @Query(value = "DELETE FROM prescription_reminder_time WHERE prescription_id IN " +
            "(SELECT id FROM prescription WHERE medicine_id NOT IN (:validIds))", nativeQuery = true)
    void deleteOrphanReminderTimes(@Param("validIds") List<Integer> validIds);

    @Modifying
    @Query(value = "DELETE FROM reminder_log WHERE prescription_id IN " +
            "(SELECT id FROM prescription WHERE medicine_id NOT IN (:validIds))", nativeQuery = true)
    void deleteOrphanReminderLogs(@Param("validIds") List<Integer> validIds);

    @Modifying
    @Query(value = "DELETE FROM prescription WHERE medicine_id NOT IN (:validIds)", nativeQuery = true)
    void deleteOrphanPrescriptions(@Param("validIds") List<Integer> validIds);

    @Modifying
    @Query(value = "DELETE FROM dispenser_slot WHERE medicine_id NOT IN (:validIds)", nativeQuery = true)
    void deleteOrphanDispenserSlots(@Param("validIds") List<Integer> validIds);

    @Modifying
    @Query(value = "DELETE FROM medicine WHERE medicine_id NOT IN (:validIds)", nativeQuery = true)
    void deleteOrphanMedicines(@Param("validIds") List<Integer> validIds);
}
