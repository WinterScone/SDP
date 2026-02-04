package org.example.sdpclient.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.example.sdpclient.configuration.WebMvcConfig;
import org.example.sdpclient.dto.AdminDto;
import org.example.sdpclient.dto.PatientRow;
import org.example.sdpclient.service.AdminListService;
import org.example.sdpclient.service.PatientAdminService;
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
    private PatientAdminService service;

    @MockitoBean
    private AdminListService adminListService;

    @Test
    void getAllPatients_shouldReturn200_andList() throws Exception {
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

        when(service.getAllPatientsSafe()).thenReturn(List.of(row));

        mockMvc.perform(get("/api/admin/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(service).getAllPatientsSafe();
    }

<<<<<<< HEAD
    @Test
    void getAllAdmins_shouldReturn200_andList() throws Exception {
        AdminDto dto = new AdminDto(1L, "admin1", "Admin", "One", "admin1@admin.com", "07700 900101", false);
        when(adminListService.getAllAdmins()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/admins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(adminListService).getAllAdmins();
    }
=======
//    @Test
//    void getAllAdmins_shouldReturn200_andList() throws Exception {
//        AdminDto dto = new AdminDto(1L, "admin1");
//        when(adminListService.getAllAdmins()).thenReturn(List.of(dto));
//
//        mockMvc.perform(get("/api/admin/admins"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$", hasSize(1)));
//
//        verify(adminListService).getAllAdmins();
//    }
>>>>>>> f770003 (add)

    @Test
    void linkAdminToPatient_shouldReturn400_whenAdminIdMissing() throws Exception {
        String body = "{}";

        mockMvc.perform(put("/api/admin/patients/10/link-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("adminId is required"));

        verifyNoInteractions(service);
    }

    @Test
    void linkAdminToPatient_shouldReturn200_andCallService_whenValid() throws Exception {
        String body = """
                {"adminId":7}
                """;

        mockMvc.perform(put("/api/admin/patients/10/link-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(service).linkAdminToPatient(10L, 7L);
    }
}

