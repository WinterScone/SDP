package org.example.sdpclient.controller;

import org.example.sdpclient.dto.*;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.service.AdminManagePatientDetailService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminManagePatientDetailController {

    private final AdminManagePatientDetailService service;

    public AdminManagePatientDetailController(AdminManagePatientDetailService service) {
        this.service = service;
    }

    @GetMapping("/patients/search")
    public List<PatientViewDto> searchPatients(@RequestParam(required = false) String q) {
        return service.searchPatients(q);
    }

    @GetMapping("/patients/{id}")
    public PatientViewDto getPatient(@PathVariable Long id) {

        var patient = service.findPatient(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        var prescriptions = service.getPrescriptionViews(id);

        return new PatientViewDto(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth(),
                patient.getEmail(),
                patient.getPhone(),
                prescriptions
        );
    }

    @GetMapping("/medicines")
    public List<MedicineViewDto> listMedicines() {
        return service.listMedicines();
    }

    @PostMapping("/patients/{id}/prescriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public void addPrescription(@PathVariable Long id, @RequestBody PrescriptionCreateDto dto) {

        if (dto == null || dto.getMedicineId() == null
                || AdminManagePatientDetailService.isBlank(dto.getDosage())
                || AdminManagePatientDetailService.isBlank(dto.getFrequency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "medicineId, dosage and frequency are required"
            );
        }

        var patient = service.findPatient(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        MedicineType medId = dto.getMedicineId();
        var medicine = service.findMedicineById(medId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found"));

        if (service.prescriptionExists(id, medId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prescription already exists for this medicine"
            );
        }

        service.createPrescription(patient, medicine, dto);
    }

    @PutMapping("/prescriptions/{id}")
    public void updatePrescription(@PathVariable Long id, @RequestBody PrescriptionUpdateDto dto) {

        if (dto == null
                || AdminManagePatientDetailService.isBlank(dto.getDosage())
                || AdminManagePatientDetailService.isBlank(dto.getFrequency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dosage and frequency are required"
            );
        }

        var rx = service.findPrescription(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found"));

        service.updatePrescription(rx, dto);
    }

    @DeleteMapping("/prescriptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePrescription(@PathVariable Long id) {

        if (!service.prescriptionIdExists(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found"
            );
        }

        service.deletePrescription(id);
    }
}
