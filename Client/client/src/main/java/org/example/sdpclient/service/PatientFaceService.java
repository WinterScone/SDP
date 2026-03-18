package org.example.sdpclient.service;

import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PatientFaceService {

    private final PatientRepository patientRepository;
    private final FaceRecognitionService faceRecognitionService;

    public PatientFaceService(PatientRepository patientRepository,
                              FaceRecognitionService faceRecognitionService) {
        this.patientRepository = patientRepository;
        this.faceRecognitionService = faceRecognitionService;
    }

    @Transactional
    public void enrollFace(Long patientId, byte[] imageBytes, String contentType) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        byte[] preparedFaceData = faceRecognitionService.prepareFaceData(imageBytes);

        patient.setFaceData(preparedFaceData);
        patient.setFaceContentType(contentType);
        patient.setFaceEnrolledAt(LocalDateTime.now());
        patient.setFaceActive(true);

        patientRepository.save(patient);
    }

    @Transactional(readOnly = true)
    public boolean verifyPatientFace(Long patientId, byte[] imageBytes) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        if (!patient.isFaceActive()) {
            return false;
        }

        if (patient.getFaceData() == null || patient.getFaceData().length == 0) {
            return false;
        }

        return faceRecognitionService.verify(patient.getFaceData(), imageBytes);
    }

    @Transactional(readOnly = true)
    public Patient identifyPatient(byte[] imageBytes) {
        List<Patient> patients = patientRepository.findByFaceActiveTrueAndFaceDataIsNotNull();

        for (Patient patient : patients) {
            if (patient.getFaceData() == null || patient.getFaceData().length == 0) {
                continue;
            }

            boolean matched = faceRecognitionService.verify(patient.getFaceData(), imageBytes);
            if (matched) {
                return patient;
            }
        }

        return null;
    }

    @Transactional(readOnly = true)
    public Patient getPatient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
    }
}