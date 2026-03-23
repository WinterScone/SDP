package org.example.sdpclient.controller;

import org.example.sdpclient.service.AdminManagePatientDetailService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Endpoint tests formerly here have moved to
 * {@link AdminPatientControllerTest} and {@link AdminPrescriptionControllerTest}.
 */
class AdminManagePatientDetailControllerTest {

    @Test
    void beanCanBeCreated() {
        var svc = mock(AdminManagePatientDetailService.class);
        var ctrl = new AdminManagePatientDetailController(svc);
        assertNotNull(ctrl);
    }
}
