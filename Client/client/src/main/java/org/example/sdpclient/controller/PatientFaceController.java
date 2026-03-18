package org.example.sdpclient.controller;

import org.example.sdpclient.dto.FaceVerifyResponse;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.service.PatientFaceService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/patient-face")
public class PatientFaceController {

    private final PatientFaceService patientFaceService;

    public PatientFaceController(PatientFaceService patientFaceService) {
        this.patientFaceService = patientFaceService;
    }

    @PostMapping(value = "/{patientId}/enroll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> enrollFace(@PathVariable Long patientId,
                                        @RequestParam("image") MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "ok", false,
                        "error", "Image file is required"
                ));
            }

            String contentType = image.getContentType();
            if (contentType == null ||
                    !(contentType.equalsIgnoreCase(MediaType.IMAGE_JPEG_VALUE)
                            || contentType.equalsIgnoreCase(MediaType.IMAGE_PNG_VALUE))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "ok", false,
                        "error", "Only JPG and PNG images are allowed"
                ));
            }

            patientFaceService.enrollFace(
                    patientId,
                    image.getBytes(),
                    contentType
            );

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "patientId", patientId,
                    "message", "Face enrolled successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "error", e.getMessage()
            ));
        }
    }
    @GetMapping("/{patientId}/image")
    public ResponseEntity<byte[]> getPatientFace(@PathVariable Long patientId) {
        Patient patient = patientFaceService.getPatient(patientId);

        if (patient.getFaceData() == null || patient.getFaceData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header("Content-Type", patient.getFaceContentType() != null
                        ? patient.getFaceContentType()
                        : "image/jpeg")
                .body(patient.getFaceData());
    }

    @PostMapping(value = "/{patientId}/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> verifyFace(@PathVariable Long patientId,
                                        @RequestParam("image") MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(new FaceVerifyResponse(
                        false,
                        false,
                        patientId,
                        "Image file is required"
                ));
            }

            boolean verified = patientFaceService.verifyPatientFace(patientId, image.getBytes());

            return ResponseEntity.ok(new FaceVerifyResponse(
                    true,
                    verified,
                    patientId,
                    verified ? "Face verified" : "Face did not match"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new FaceVerifyResponse(
                    false,
                    false,
                    patientId,
                    e.getMessage()
            ));
        }
    }

    @PostMapping(value = "/identify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> identifyFace(@RequestParam("image") MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "ok", false,
                        "matched", false,
                        "error", "Image file is required"
                ));
            }

            Patient patient = patientFaceService.identifyPatient(image.getBytes());

            if (patient == null) {
                return ResponseEntity.ok(Map.of(
                        "ok", true,
                        "matched", false,
                        "message", "No matching patient found"
                ));
            }

            var prescriptions = patient.getPrescriptions().stream()
                    .map(p -> Map.of(
                            "prescriptionId", p.getId(),
                            "medicineCode", p.getMedicine().getMedicineId().name(),
                            "medicineName", p.getMedicine().getMedicineName(),
                            "dosage", p.getDosage(),
                            "frequency", p.getFrequency(),
                            "scheduledTimes", p.getReminderTimes().stream()
                                    .map(rt -> rt.getReminderTime().toString())
                                    .toList()
                    ))
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "matched", true,
                    "patientId", patient.getId(),
                    "firstName", patient.getFirstName(),
                    "lastName", patient.getLastName(),
                    "patientName", patient.getFirstName() + " " + patient.getLastName(),
                    "prescriptions", prescriptions,
                    "message", "Patient identified successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "matched", false,
                    "error", e.getMessage()
            ));
        }
    }


}