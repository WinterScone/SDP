package org.example.sdpclient.controller;

import org.example.sdpclient.service.AdminManagePatientDetailService;
import org.springframework.stereotype.Component;

/**
 * Endpoints formerly here have been consolidated into
 * {@link AdminPatientController} (search / get-patient) and
 * {@link AdminPrescriptionController} (medicines / prescriptions CRUD).
 */
@Component
public class AdminManagePatientDetailController {

    private final AdminManagePatientDetailService service;

    public AdminManagePatientDetailController(AdminManagePatientDetailService service) {
        this.service = service;
    }
}
