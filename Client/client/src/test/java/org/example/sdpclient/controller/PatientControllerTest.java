package org.example.sdpclient.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.example.sdpclient.configuration.WebMvcConfig;
import org.example.sdpclient.dto.PatientPrescriptionsResponse;
import org.example.sdpclient.service.PatientDetailService;
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
        controllers = PatientController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WebMvcConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientDetailService patientDetailService;

    @Test
    void getPatientPrescriptions_shouldReturn400_whenPatientNotFound() throws Exception {
        when(patientDetailService.patientExists(10L)).thenReturn(false);

        mockMvc.perform(get("/api/patients/10/prescriptions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").value("Patient not found"));

        verify(patientDetailService).patientExists(10L);
        verify(patientDetailService, never()).getPrescriptionItems(anyLong());
    }

    @Test
    void getPatientPrescriptions_shouldReturn200_withResponse_whenPatientExists() throws Exception {
        when(patientDetailService.patientExists(10L)).thenReturn(true);

        PatientPrescriptionsResponse.PrescriptionItem item =
                new PatientPrescriptionsResponse.PrescriptionItem(
                        "MEDICINE_ID1",
                        "TestMed",
                        "10mg",
                        1
                );

        when(patientDetailService.getPrescriptionItems(10L)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/patients/10/prescriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(10))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MEDICINE_ID1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TestMed")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("10mg")))
                .andExpect(jsonPath("$.prescriptions[0].frequency").value(1));

        verify(patientDetailService).patientExists(10L);
        verify(patientDetailService).getPrescriptionItems(10L);
    }

    @Test
    void getCollectableMedications_shouldReturn400_whenPatientNotFound() throws Exception {
        when(patientDetailService.patientExists(10L)).thenReturn(false);

        mockMvc.perform(get("/api/patients/10/collectable")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").value("Patient not found"));

        verify(patientDetailService).patientExists(10L);
        verify(patientDetailService, never()).getCollectableItems(anyLong());
    }

    @Test
    void getCollectableMedications_shouldReturn200_withItems_whenPatientExists() throws Exception {
        when(patientDetailService.patientExists(10L)).thenReturn(true);

        PatientPrescriptionsResponse.PrescriptionItem item =
                new PatientPrescriptionsResponse.PrescriptionItem(
                        1L, 3, "101", "Amoxicillin", "10mg", 1, "08:00:00", 1
                );

        when(patientDetailService.getCollectableItems(10L)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/patients/10/collectable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(10))
                .andExpect(jsonPath("$.prescriptions[0].prescriptionId").value(1))
                .andExpect(jsonPath("$.prescriptions[0].medicineName").value("Amoxicillin"))
                .andExpect(jsonPath("$.prescriptions[0].scheduledTime").value("08:00:00"));

        verify(patientDetailService).patientExists(10L);
        verify(patientDetailService).getCollectableItems(10L);
    }

    @Test
    void getCollectableMedications_shouldReturn200_withEmptyList_whenNothingDue() throws Exception {
        when(patientDetailService.patientExists(10L)).thenReturn(true);
        when(patientDetailService.getCollectableItems(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/patients/10/collectable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(10))
                .andExpect(jsonPath("$.prescriptions").isEmpty());

        verify(patientDetailService).getCollectableItems(10L);
    }
}
