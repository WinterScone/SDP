package org.example.sdpclient.repository;

import org.example.sdpclient.entity.PatientImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientImageRepository extends JpaRepository<PatientImage, Long> {

    /**
     * Fetches an image only when both the imageId AND the patientId match,
     * preventing IDOR — callers cannot access images belonging to other patients
     * by guessing an imageId.
     */
    Optional<PatientImage> findByIdAndPatient_Id(Long id, Long patientId);
}
