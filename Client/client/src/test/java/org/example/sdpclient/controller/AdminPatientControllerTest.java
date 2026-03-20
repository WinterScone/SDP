package org.example.sdpclient.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.example.sdpclient.configuration.WebMvcConfig;
import org.example.sdpclient.dto.*;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.service.AdminManagePatientDetailService;
import org.example.sdpclient.service.PatientAdminService;
import org.example.sdpclient.service.PatientDetailService;
import org.example.sdpclient.service.PatientImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = AdminPatientController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WebMvcConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AdminPatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientAdminService patientAdminService;

    @MockitoBean
    private PatientDetailService patientDetailService;

    @MockitoBean
    private AdminManagePatientDetailService adminManageService;

    @MockitoBean
    private PatientImageService patientImageService;

    // -------------------------
    // GET /api/admin/patients (getAllPatients)
    // -------------------------

    @Test
    void getAllPatients_shouldReturn200_andList() throws Exception {
        Patient p = new Patient();
        p.setId(1L);
        p.setFirstName("John");
        p.setLastName("Doe");
        p.setDateOfBirth(LocalDate.of(2000, 1, 1).toString());
        p.setEmail("john@example.com");
        p.setPhone("123");

        when(patientDetailService.getAllPatientsForAdmin(anyLong(), anyBoolean())).thenReturn(List.of(p));

        mockMvc.perform(get("/api/admin/patients")
                        .cookie(new jakarta.servlet.http.Cookie("adminId", "1"))
                        .cookie(new jakarta.servlet.http.Cookie("adminRoot", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(patientDetailService).getAllPatientsForAdmin(anyLong(), anyBoolean());
    }

    // -------------------------
    // GET /api/admin/patients/assignments (root only)
    // -------------------------

    @Test
    void getPatientAssignments_shouldReturn200_andList() throws Exception {
        PatientRow row = new PatientRow(
                1L,
                "John",
                "Doe",
                LocalDate.of(2000, 1, 1).toString(),
                "john@example.com",
                "123",
                LocalDateTime.of(2024, 1, 1, 10, 0).toString(),
                null,
                null
        );

        when(patientAdminService.getAllPatientsSafe()).thenReturn(List.of(row));

        mockMvc.perform(get("/api/admin/patients/assignments")
                        .cookie(new jakarta.servlet.http.Cookie("adminRoot", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(patientAdminService).getAllPatientsSafe();
    }

    // -------------------------
    // GET /api/admin/patients/search
    // -------------------------

    @Test
    void searchPatients_shouldReturn200_andDelegateToService() throws Exception {
        PatientViewDto dto = new PatientViewDto(
                1L, "John", "Doe", "2000-01-01", "j@e.com", "123", List.of()
        );

        when(adminManageService.searchPatients(anyString(), anyLong(), anyBoolean())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/patients/search").param("q", "abc")
                        .cookie(new jakarta.servlet.http.Cookie("adminId", "1"))
                        .cookie(new jakarta.servlet.http.Cookie("adminRoot", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(adminManageService).searchPatients(anyString(), anyLong(), anyBoolean());
    }

    // -------------------------
    // GET /api/admin/patients/{id}
    // -------------------------

    @Test
    void getPatient_shouldReturn404_whenPatientNotFound() throws Exception {
        when(adminManageService.canAdminAccessPatient(anyLong(), anyLong(), anyBoolean())).thenReturn(true);
        when(adminManageService.findPatient(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/patients/10")
                        .cookie(new jakarta.servlet.http.Cookie("adminId", "1"))
                        .cookie(new jakarta.servlet.http.Cookie("adminRoot", "true")))
                .andExpect(status().isNotFound());

        verify(adminManageService).findPatient(10L);
        verify(adminManageService, never()).getPrescriptionViews(anyLong());
    }

    // -------------------------
    // GET /api/admin/patients/images
    // -------------------------

    @Test
    void getAllPatientImages_shouldReturn200_andList() throws Exception {
        PatientImageDto dto = new PatientImageDto("john", "data:image/png;base64,abc123", "image/png");
        PatientImageDto dtoNoImage = new PatientImageDto("jane", null, null);
        when(patientImageService.getAllPatientImages()).thenReturn(List.of(dto, dtoNoImage));

        mockMvc.perform(get("/api/admin/patients/images")
                        .cookie(new jakarta.servlet.http.Cookie("adminRoot", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("john"))
                .andExpect(jsonPath("$[0].image").value("data:image/png;base64,abc123"))
                .andExpect(jsonPath("$[0].contentType").value("image/png"))
                .andExpect(jsonPath("$[1].username").value("jane"))
                .andExpect(jsonPath("$[1].image").isEmpty());

        verify(patientImageService).getAllPatientImages();
    }

    @Test
    void getAllPatientImages_shouldReturn403_whenNotRootAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/patients/images")
                        .cookie(new jakarta.servlet.http.Cookie("adminRoot", "false")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(patientImageService);
    }

    // -------------------------
    // PUT /api/admin/patients/{id}/link-admin
    // -------------------------

    @Test
    void linkAdminToPatient_shouldReturn400_whenAdminIdMissing() throws Exception {
        String body = "{}";

        mockMvc.perform(put("/api/admin/patients/10/link-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(new jakarta.servlet.http.Cookie("adminRoot", "true"))
                        .cookie(new jakarta.servlet.http.Cookie("adminId", "1"))
                        .cookie(new jakarta.servlet.http.Cookie("adminUsername", "root")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("adminId is required"));

        verifyNoInteractions(patientAdminService);
    }

    @Test
    void linkAdminToPatient_shouldReturn200_andCallService_whenValid() throws Exception {
        String body = """
                {"adminId":7}
                """;

        mockMvc.perform(put("/api/admin/patients/10/link-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(new jakarta.servlet.http.Cookie("adminRoot", "true"))
                        .cookie(new jakarta.servlet.http.Cookie("adminId", "1"))
                        .cookie(new jakarta.servlet.http.Cookie("adminUsername", "root")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(patientAdminService).linkAdminToPatient(eq(10L), eq(7L), any(), any());
    }
}
