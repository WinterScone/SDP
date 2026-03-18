package org.example.sdpclient.controller;

import org.example.sdpclient.dto.DispenseResultRequest;
import org.example.sdpclient.dto.DispenseResultResponse;
import org.example.sdpclient.dto.MachineIdentifyResponse;
import org.example.sdpclient.service.MachineAuthService;
import org.example.sdpclient.service.MachineService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/machine")
public class MachineController {

    private final MachineService machineService;
    private final MachineAuthService machineAuthService;

    public MachineController(MachineService machineService,
                             MachineAuthService machineAuthService) {
        this.machineService = machineService;
        this.machineAuthService = machineAuthService;
    }

    @PostMapping(value = "/identify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MachineIdentifyResponse> identify(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestParam("image") MultipartFile image) {

        if (!machineAuthService.isValid(apiKey)) {
            return ResponseEntity.status(401).body(
                    new MachineIdentifyResponse(
                            false,
                            false,
                            null,
                            null,
                            null,
                            null,
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
                                "Image file is required"
                        )
                );
            }

            MachineIdentifyResponse response =
                    machineService.identifyPatientAndLoadPrescriptions(image.getBytes());

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
                            e.getMessage()
                    )
            );
        }
    }

    @PostMapping("/dispense-result")
    public ResponseEntity<DispenseResultResponse> saveDispenseResult(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestBody DispenseResultRequest request) {

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