package org.example.sdpclient.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;

import org.example.sdpclient.configuration.WebMvcConfig;
import org.example.sdpclient.dto.IntakeLogRequest;
import org.example.sdpclient.service.IntakeHistoryService;
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
        controllers = IntakeHistoryController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WebMvcConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class IntakeHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntakeHistoryService intakeHistoryService;

    @Test
    void logIntake_shouldReturn200_whenOk() throws Exception {
        when(intakeHistoryService.logIntake(eq(1L), any(IntakeLogRequest.class)))
                .thenReturn(Map.of("ok", true));

        mockMvc.perform(post("/api/patients/1/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineId\":\"VTM01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void logIntake_shouldReturn400_whenNotOk() throws Exception {
        when(intakeHistoryService.logIntake(eq(1L), any(IntakeLogRequest.class)))
                .thenReturn(Map.of("ok", false, "error", "Patient not found"));

        mockMvc.perform(post("/api/patients/1/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineId\":\"VTM01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void getHistory_shouldReturn200() throws Exception {
        when(intakeHistoryService.getHistory(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/patients/1/intake"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(1))
                .andExpect(jsonPath("$.history").isArray());

        verify(intakeHistoryService).getHistory(1L);
    }
}
