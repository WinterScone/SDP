package org.example.sdpclient.repository;

import org.example.sdpclient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUsername(String username);

    boolean existsByUsername(String username);
}
