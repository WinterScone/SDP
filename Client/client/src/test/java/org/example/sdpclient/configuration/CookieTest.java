package org.example.sdpclient.configuration;

import org.example.sdpclient.controller.AdminAuthController;
import org.example.sdpclient.controller.AdminController;
import org.example.sdpclient.controller.AdminPatientController;
import org.example.sdpclient.controller.AdminPrescriptionController;
import org.example.sdpclient.repository.AdminRepository;
import org.example.sdpclient.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        AdminPatientController.class,
        AdminPrescriptionController.class,
        AdminController.class,
        AdminAuthController.class
})
class CookieTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private PatientAdminService patientAdminService;
    @MockitoBean private PatientDetailService patientDetailService;
    @MockitoBean private AdminManagePatientDetailService adminManagePatientDetailService;
    @MockitoBean private AdminLoginService adminLoginService;
    @MockitoBean private AdminRepository adminRepository;
    @MockitoBean private DatabaseResetService databaseResetService;
    @MockitoBean private PatientImageService patientImageService;
    @MockitoBean private AdminListService adminListService;
    @MockitoBean private ActivityLogService activityLogService;
    @MockitoBean private SmsService smsService;

    @Test
    void noCookie_getPatients_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void noCookie_getAdmins_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/admins"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void noCookie_searchPatients_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/patients/search").param("q", "test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void noCookie_getPatientById_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/patients/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void noCookie_getMedicines_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/medicines"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void noCookie_addPrescription_returns401() throws Exception {
        mockMvc.perform(post("/api/admin/patients/1/prescriptions")
                        .contentType("application/json")
                        .content("{\"medicineId\":\"ASPIRIN\",\"dosage\":\"100mg\",\"frequency\":\"once\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void noCookie_updatePrescription_returns401() throws Exception {
        mockMvc.perform(put("/api/admin/prescriptions/1")
                        .contentType("application/json")
                        .content("{\"dosage\":\"200mg\",\"frequency\":\"twice\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void noCookie_deletePrescription_returns401() throws Exception {
        mockMvc.perform(delete("/api/admin/prescriptions/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void noCookie_linkAdmin_returns401() throws Exception {
        mockMvc.perform(put("/api/admin/patients/1/link-admin")
                        .contentType("application/json")
                        .content("{\"adminId\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void withCookie_getPatients_passes() throws Exception {
        mockMvc.perform(get("/api/admin/patients")
                        .cookie(new MockCookie("adminUsername", "admin1"))
                        .cookie(new MockCookie("adminId", "1"))
                        .cookie(new MockCookie("adminRoot", "true")))
                .andExpect(status().isOk());
    }

    @Test
    void withCookie_getAdmins_passes() throws Exception {
        mockMvc.perform(get("/api/admin/admins")
                        .cookie(new MockCookie("adminUsername", "admin1")))
                .andExpect(status().isOk());
    }

    @Test
    void withCookie_searchPatients_passes() throws Exception {
        mockMvc.perform(get("/api/admin/patients/search").param("q", "test")
                        .cookie(new MockCookie("adminUsername", "admin1"))
                        .cookie(new MockCookie("adminId", "1"))
                        .cookie(new MockCookie("adminRoot", "false")))
                .andExpect(status().isOk());
    }

    @Test
    void withCookie_getMedicines_passes() throws Exception {
        mockMvc.perform(get("/api/admin/medicines")
                        .cookie(new MockCookie("adminUsername", "admin1")))
                .andExpect(status().isOk());
    }

    @Test
    void noCookie_authMe_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/admins/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void withCookie_authMe_returns200WithUsername() throws Exception {
        mockMvc.perform(get("/api/auth/admins/me")
                        .cookie(new MockCookie("adminUsername", "admin1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.username").value("admin1"));
    }

    @Test
    void noCookie_register_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/admins/register")
                        .contentType("application/json")
                        .content("{\"username\":\"newadmin\",\"password\":\"pass123\",\"firstName\":\"New\",\"lastName\":\"Admin\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void nonRootCookie_register_returns403() throws Exception {
        org.example.sdpclient.entity.Admin nonRoot = new org.example.sdpclient.entity.Admin();
        nonRoot.setUsername("admin1");
        nonRoot.setRoot(false);

        org.mockito.Mockito.when(adminRepository.findByUsername("admin1"))
                .thenReturn(java.util.Optional.of(nonRoot));

        mockMvc.perform(post("/api/auth/admins/register")
                        .contentType("application/json")
                        .content("{\"username\":\"newadmin\",\"password\":\"pass123\",\"firstName\":\"New\",\"lastName\":\"Admin\"}")
                        .cookie(new MockCookie("adminUsername", "admin1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void rootCookie_register_passesThrough() throws Exception {
        org.example.sdpclient.entity.Admin root = new org.example.sdpclient.entity.Admin();
        root.setUsername("root");
        root.setRoot(true);

        org.mockito.Mockito.when(adminRepository.findByUsername("root"))
                .thenReturn(java.util.Optional.of(root));
        org.mockito.Mockito.when(adminLoginService.register(org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                .thenReturn(java.util.Map.of("ok", true, "username", "newadmin"));

        mockMvc.perform(post("/api/auth/admins/register")
                        .contentType("application/json")
                        .content("{\"username\":\"newadmin\",\"password\":\"pass123\",\"firstName\":\"New\",\"lastName\":\"Admin\"}")
                        .cookie(new MockCookie("adminUsername", "root")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.username").value("newadmin"));
    }
}
