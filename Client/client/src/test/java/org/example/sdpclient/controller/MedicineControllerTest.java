package org.example.sdpclient.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.example.sdpclient.configuration.WebMvcConfig;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.service.MedicineService;
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
        controllers = MedicineController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WebMvcConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class MedicineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedicineService service;

    // -------------------------
    // GET /api/medicines
    // -------------------------

    @Test
    void getAll_shouldReturn200_andList() throws Exception {
        Medicine med = new Medicine();
        med.setMedicineId(MedicineType.MEDICINE_ID1);
        med.setMedicineName("Amoxicillin");
        med.setQuantity(10);

        when(service.getAll()).thenReturn(List.of(med));

        mockMvc.perform(get("/api/medicines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(service).getAll();
    }

    // -------------------------
    // PATCH /api/medicines/{id}/quantity
    // -------------------------

    @Test
    void updateQuantity_shouldReturn400_whenQuantityMissing() throws Exception {
        mockMvc.perform(patch("/api/medicines/MEDICINE_ID1/quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value("Quantity must be >= 0"));

        verifyNoInteractions(service);
    }

    @Test
    void updateQuantity_shouldReturn400_whenQuantityNegative() throws Exception {
        mockMvc.perform(patch("/api/medicines/MEDICINE_ID1/quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value("Quantity must be >= 0"));

        verifyNoInteractions(service);
    }

    @Test
    void updateQuantity_shouldReturn400_whenMedicineNotFound() throws Exception {
        when(service.exists(MedicineType.MEDICINE_ID1)).thenReturn(false);

        mockMvc.perform(patch("/api/medicines/MEDICINE_ID1/quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value("Not found"));

        verify(service).exists(MedicineType.MEDICINE_ID1);
        verify(service, never()).updateQuantity(any(), anyInt());
    }

    @Test
    void updateQuantity_shouldReturn200_whenUpdated() throws Exception {
        when(service.exists(MedicineType.MEDICINE_ID1)).thenReturn(true);

        mockMvc.perform(patch("/api/medicines/MEDICINE_ID1/quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(service).exists(MedicineType.MEDICINE_ID1);
        verify(service).updateQuantity(MedicineType.MEDICINE_ID1, 20);
    }
}

