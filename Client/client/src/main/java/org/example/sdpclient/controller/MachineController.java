package org.example.sdpclient.controller;

import org.example.sdpclient.dto.DispenseResultRequest;
import org.example.sdpclient.dto.DispenseResultResponse;
import org.example.sdpclient.dto.MachineIdentifyResponse;
import org.example.sdpclient.dto.MachinePatientImagesResponse;
import org.example.sdpclient.service.MachineAuthService;
import org.example.sdpclient.service.MachineService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/machine")
public class MachineController {

    private final MachineService machineService;
    private final MachineAuthService machineAuthService;

    public MachineController(MachineService machineService, MachineAuthService machineAuthService) {
        this.machineService = machineService;
        this.machineAuthService = machineAuthService;
    }

    @GetMapping("/patient-images")
    public ResponseEntity<?> getAllPatientImages(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey
    ) {
        if (!machineAuthService.isValid(apiKey)) {
            return ResponseEntity.status(401).body(
                    new MachinePatientImagesResponse(false, 0, List.of(), "Unauthorized machine request")
            );
        }

        try {
            return ResponseEntity.ok(machineService.getAllPatientImages());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new MachinePatientImagesResponse(false, 0, List.of(), e.getMessage())
            );
        }
    }

    @GetMapping("/patients/{patientId}/prescriptions")
    public ResponseEntity<?> getPatientPrescriptions(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @PathVariable Long patientId
    ) {
        if (!machineAuthService.isValid(apiKey)) {
            return ResponseEntity.status(401).body(
                    new MachineIdentifyResponse(
                            false,
                            false,
                            null,
                            null,
                            null,
                            null,
                            List.of(),
                            "Unauthorized machine request"
                    )
            );
        }

        try {
            MachineIdentifyResponse response = machineService.getPatientPrescriptionsByPatientId(patientId);

            if (!response.isOk()) {
                return ResponseEntity.status(404).body(response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new MachineIdentifyResponse(
                            false,
                            false,
                            null,
                            null,
                            null,
                            null,
                            List.of(),
                            e.getMessage()
                    )
            );
        }
    }

    @PostMapping(value = "/identify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> identify(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestParam("image") MultipartFile image
    ) {
        if (!machineAuthService.isValid(apiKey)) {
            return ResponseEntity.status(401).body(
                    new MachineIdentifyResponse(
                            false,
                            false,
                            null,
                            null,
                            null,
                            null,
                            List.of(),
                            "Unauthorized machine request"
                    )
            );
        }

        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new MachineIdentifyResponse(
                                false,
                                false,
                                null,
                                null,
                                null,
                                null,
                                List.of(),
                                "Image file is required"
                        )
                );
            }

            MachineIdentifyResponse response = machineService.identifyPatientAndLoadPrescriptions(image.getBytes());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new MachineIdentifyResponse(
                            false,
                            false,
                            null,
                            null,
                            null,
                            null,
                            List.of(),
                            e.getMessage()
                    )
            );
        }
    }

    @PostMapping("/dispense-result")
    public ResponseEntity<?> saveDispenseResult(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestBody DispenseResultRequest request
    ) {
        if (!machineAuthService.isValid(apiKey)) {
            return ResponseEntity.status(401).body(
                    new DispenseResultResponse(false, "Unauthorized machine request")
            );
        }

        try {
            DispenseResultResponse response = machineService.saveDispenseResult(request);
            if (!response.isOk()) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new DispenseResultResponse(false, e.getMessage())
            );
        }
    }
}