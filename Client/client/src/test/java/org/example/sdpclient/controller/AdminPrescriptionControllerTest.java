package org.example.sdpclient.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import org.example.sdpclient.configuration.WebMvcConfig;
import org.example.sdpclient.dto.*;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.service.AdminManagePatientDetailService;
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
        controllers = AdminPrescriptionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WebMvcConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AdminPrescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminManagePatientDetailService service;

    @Test
    void listMedicines_shouldReturn200_andDelegateToService() throws Exception {
        MedicineViewDto med = new MedicineViewDto(MedicineType.VTM01, "TestMed", null, null);
        when(service.listMedicines()).thenReturn(List.of(med));

        mockMvc.perform(get("/api/admin/medicines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(service).listMedicines();
    }

    @Test
    void addPrescription_shouldReturn400_whenBodyMissingRequiredFields() throws Exception {
        String body = """
                {"medicineId":"VTM01","dosage":"  ","frequency":""}
                """;

        when(service.canAdminAccessPatient(anyLong(), anyLong(), anyBoolean())).thenReturn(true);

        mockMvc.perform(post("/api/admin/patients/10/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(new jakarta.servlet.http.Cookie("adminId", "1"))
                        .cookie(new jakarta.servlet.http.Cookie("adminRoot", "true")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addPrescription_shouldReturn201_whenCreated() throws Exception {
        String body = """
                {"medicineId":"VTM01","dosage":"1000","frequency":"ONCE_A_DAY","scheduledTimes":["08:00","20:00"]}
                """;

        Patient patient = new Patient();
        patient.setId(10L);

        Medicine med = new Medicine();
        med.setMedicineId(MedicineType.VTM01);
        med.setMedicineName("TestMed");
        med.setUnitDose(1000);

        when(service.canAdminAccessPatient(anyLong(), anyLong(), anyBoolean())).thenReturn(true);
        when(service.findPatient(10L)).thenReturn(Optional.of(patient));
        when(service.findMedicineById(MedicineType.VTM01)).thenReturn(Optional.of(med));
        when(service.prescriptionExists(10L, MedicineType.VTM01)).thenReturn(false);

        mockMvc.perform(post("/api/admin/patients/10/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(new jakarta.servlet.http.Cookie("adminId", "1"))
                        .cookie(new jakarta.servlet.http.Cookie("adminRoot", "true")))
                .andExpect(status().isCreated());

        verify(service).createPrescription(eq(patient), eq(med), any(PrescriptionCreateDto.class), any(), any());
    }

    @Test
    void updatePrescription_shouldReturn200_whenUpdated() throws Exception {
        String body = """
                {"dosage":"1000","frequency":"ONCE_A_DAY"}
                """;

        Patient patient = new Patient();
        patient.setId(10L);

        Medicine med = new Medicine();
        med.setMedicineId(MedicineType.VTM01);
        med.setUnitDose(1000);

        Prescription rx = new Prescription();
        rx.setId(5L);
        rx.setPatient(patient);
        rx.setMedicine(med);

        when(service.findPrescription(5L)).thenReturn(Optional.of(rx));
        when(service.canAdminAccessPatient(anyLong(), anyLong(), anyBoolean())).thenReturn(true);

        mockMvc.perform(put("/api/admin/prescriptions/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(new jakarta.servlet.http.Cookie("adminId", "1"))
                        .cookie(new jakarta.servlet.http.Cookie("adminRoot", "true")))
                .andExpect(status().isOk());

        verify(service).updatePrescription(eq(rx), any(PrescriptionUpdateDto.class));
    }

    @Test
    void deletePrescription_shouldReturn204_whenDeleted() throws Exception {
        Patient patient = new Patient();
        patient.setId(10L);

        Prescription rx = new Prescription();
        rx.setId(5L);
        rx.setPatient(patient);

        when(service.findPrescription(5L)).thenReturn(Optional.of(rx));
        when(service.canAdminAccessPatient(anyLong(), anyLong(), anyBoolean())).thenReturn(true);

        mockMvc.perform(delete("/api/admin/prescriptions/5")
                        .cookie(new jakarta.servlet.http.Cookie("adminId", "1"))
                        .cookie(new jakarta.servlet.http.Cookie("adminRoot", "true")))
                .andExpect(status().isNoContent());

        verify(service).deletePrescription(eq(5L), any(), any());
    }
}
