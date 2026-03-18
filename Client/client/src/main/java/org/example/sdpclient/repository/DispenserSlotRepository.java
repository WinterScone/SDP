package org.example.sdpclient.repository;

import org.example.sdpclient.entity.DispenserSlot;
import org.example.sdpclient.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DispenserSlotRepository extends JpaRepository<DispenserSlot, Long> {

    Optional<DispenserSlot> findByMedicineAndActiveTrue(Medicine medicine);

    Optional<DispenserSlot> findBySlotNumber(Integer slotNumber);
}