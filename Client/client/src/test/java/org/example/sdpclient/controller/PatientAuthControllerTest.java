package org.example.sdpclient.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import org.example.sdpclient.configuration.WebMvcConfig;
import org.example.sdpclient.service.PatientLoginService;
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
        controllers = PatientAuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WebMvcConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class PatientAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientLoginService service;

    // -------------------------
    // POST /api/auth/patients/login
    // -------------------------

    @Test
    void login_shouldReturn400_whenOkFalse() throws Exception {
        when(service.login(any())).thenReturn(Map.of("ok", false));

        mockMvc.perform(post("/api/auth/patients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"password\":\"p\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(service).login(any());
    }

    @Test
    void login_shouldReturn200_whenOkTrue() throws Exception {
        when(service.login(any())).thenReturn(Map.of(
                "ok", true,
                "username", "u",
                "patientId", 10
        ));

        mockMvc.perform(post("/api/auth/patients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"password\":\"p\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.username").value("u"))
                .andExpect(jsonPath("$.patientId").value(10));

        verify(service).login(any());
    }

    // -------------------------
    // POST /api/auth/patients/signup
    // -------------------------

    @Test
    void signup_shouldReturn400_whenUsernameAlreadyTaken() throws Exception {
        when(service.signup(any())).thenReturn(Map.of(
                "ok", false,
                "error", "Username already taken"
        ));

        mockMvc.perform(post("/api/auth/patients/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"u",
                                  "password":"p",
                                  "firstName":"a",
                                  "lastName":"b",
                                  "dateOfBirth":"2000-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").value("Username already taken"));

        verify(service).signup(any());
    }

    @Test
    void signup_shouldReturn400_whenOtherError() throws Exception {
        when(service.signup(any())).thenReturn(Map.of(
                "ok", false,
                "error", "Missing required fields"
        ));

        mockMvc.perform(post("/api/auth/patients/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"",
                                  "password":"",
                                  "firstName":"",
                                  "lastName":"",
                                  "dateOfBirth":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").value("Missing required fields"));

        verify(service).signup(any());
    }

    @Test
    void signup_shouldReturn200_whenOkTrue() throws Exception {
        when(service.signup(any())).thenReturn(Map.of(
                "ok", true,
                "id", 99,
                "username", "u"
        ));

        mockMvc.perform(post("/api/auth/patients/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"u",
                                  "password":"p",
                                  "firstName":"a",
                                  "lastName":"b",
                                  "dateOfBirth":"2000-01-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.username").value("u"));

        verify(service).signup(any());
    }
}
