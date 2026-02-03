package org.example.sdpclient.controller;


import static org.hamcrest.Matchers.hasSize;
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
        controllers = PatientDetailController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WebMvcConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class PatientDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientDetailService service;

    @Test
    void getPatientPrescriptions_shouldReturn400_whenPatientNotFound() throws Exception {
        when(service.patientExists(10L)).thenReturn(false);

        mockMvc.perform(get("/api/patient/10/prescriptions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").value("Patient not found"));

        verify(service).patientExists(10L);
        verify(service, never()).getPrescriptionItems(anyLong());
    }

    @Test
    void getPatientPrescriptions_shouldReturn200_withResponse_whenPatientExists() throws Exception {
        when(service.patientExists(10L)).thenReturn(true);

        PatientPrescriptionsResponse.PrescriptionItem item =
                new PatientPrescriptionsResponse.PrescriptionItem(
                        "MEDICINE_ID1",
                        "TestMed",
                        "10mg",
                        "daily"
                );

        when(service.getPrescriptionItems(10L)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/patient/10/prescriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(10))
                // ✅ don't assume the list property name; just check content is present
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MEDICINE_ID1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TestMed")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("10mg")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("daily")));

        verify(service).patientExists(10L);
        verify(service).getPrescriptionItems(10L);
    }

}

