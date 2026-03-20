package org.example.sdpclient.service;

import org.example.sdpclient.dto.PatientImageDto;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.PatientImage;
import org.example.sdpclient.repository.PatientImageRepository;
import org.example.sdpclient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PatientImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private final PatientImageRepository imageRepo;
    private final PatientRepository patientRepo;

    public PatientImageService(PatientImageRepository imageRepo, PatientRepository patientRepo) {
        this.imageRepo = imageRepo;
        this.patientRepo = patientRepo;
    }

    public boolean patientExists(Long patientId) {
        return patientRepo.existsById(patientId);
    }

    /**
     * Fetches an image by (imageId, patientId) together so that an imageId can
     * never be used to read another patient's data (IDOR prevention).
     * Returns empty when either the image does not exist or it belongs to a
     * different patient.
     */
    @Transactional(readOnly = true)
    public Optional<PatientImage> findImage(Long patientId, Long imageId) {
        return imageRepo.findByIdAndPatient_Id(imageId, patientId)
                .filter(img -> ALLOWED_CONTENT_TYPES.contains(img.getContentType()));
    }

    @Transactional(readOnly = true)
    public List<PatientImageDto> getAllPatientImages() {
        List<Patient> patients = patientRepo.findAll();
        Map<Long, PatientImage> imagesByPatientId = imageRepo.findAll().stream()
                .collect(Collectors.toMap(img -> img.getPatient().getId(), Function.identity()));

        return patients.stream().map(patient -> {
            PatientImage img = imagesByPatientId.get(patient.getId());
            String image = null;
            String contentType = null;
            if (img != null && ALLOWED_CONTENT_TYPES.contains(img.getContentType())) {
                contentType = img.getContentType();
                image = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(img.getData());
            }
            return new PatientImageDto(patient.getUsername(), image, contentType);
        }).toList();
    }
}
