package org.example.sdpclient.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import org.example.sdpclient.configuration.WebMvcConfig;
import org.example.sdpclient.entity.Admin;
import org.example.sdpclient.repository.AdminRepository;
import org.example.sdpclient.service.AdminLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;

@WebMvcTest(
        controllers = AdminVerificationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WebMvcConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AdminVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminLoginService service;

    @MockitoBean
    private AdminRepository adminRepo;

    // -------------------------
    // POST /api/verify/login
    // -------------------------

    @Test
    void login_shouldReturn400_whenServiceOkFalse() throws Exception {
        when(service.login(any())).thenReturn(Map.of("ok", false));

        mockMvc.perform(post("/api/verify/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"y\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(service).login(any());
    }

    @Test
    void login_shouldReturn200_andSetCookies_whenServiceOkTrue() throws Exception {
        when(service.login(any())).thenReturn(Map.of(
                "ok", true,
                "username", "admin1",
                "root", true
        ));

        mockMvc.perform(post("/api/verify/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin1\",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(result -> {
                    var setCookies = result.getResponse().getHeaders("Set-Cookie");

                    // Must contain BOTH cookies somewhere in the Set-Cookie headers
                    assertTrue(setCookies.stream().anyMatch(h -> h.contains("adminUsername=admin1")),
                            "Expected Set-Cookie to contain adminUsername=admin1 but was: " + setCookies);

                    assertTrue(setCookies.stream().anyMatch(h -> h.contains("adminRoot=true")),
                            "Expected Set-Cookie to contain adminRoot=true but was: " + setCookies);
                });

        verify(service).login(any());
    }


    // -------------------------
    // POST /api/verify/logout
    // -------------------------

    @Test
    void logout_shouldReturn200_andClearCookies() throws Exception {
        mockMvc.perform(post("/api/verify/logout"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    var setCookies = result.getResponse().getHeaders("Set-Cookie");

                    assertTrue(setCookies.stream().anyMatch(h -> h.contains("adminUsername=") && h.contains("Max-Age=0")),
                            "Expected adminUsername cleared but was: " + setCookies);

                    assertTrue(setCookies.stream().anyMatch(h -> h.contains("adminRoot=") && h.contains("Max-Age=0")),
                            "Expected adminRoot cleared but was: " + setCookies);
                });
    }


    // -------------------------
    // GET /api/verify/me
    // -------------------------

    @Test
    void me_shouldReturn401_whenNoCookie() throws Exception {
        mockMvc.perform(get("/api/verify/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void me_shouldReturn200_whenCookiePresent() throws Exception {
        mockMvc.perform(get("/api/verify/me")
                        .cookie(new Cookie("adminUsername", "admin1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.username").value("admin1"));
    }

    // -------------------------
    // POST /api/verify/register
    // -------------------------

    @Test
    void register_shouldReturn401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/verify/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new\",\"password\":\"pw\",\"firstName\":\"a\",\"lastName\":\"b\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        verifyNoInteractions(adminRepo);
        verify(service, never()).register(any(), any(), any());
    }

    @Test
    void register_shouldReturn403_whenCallerNotRoot() throws Exception {
        Admin caller = new Admin();
        caller.setUsername("admin1");
        caller.setRoot(false);

        when(adminRepo.findByUsername("admin1")).thenReturn(Optional.of(caller));

        mockMvc.perform(post("/api/verify/register")
                        .cookie(new Cookie("adminUsername", "admin1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new\",\"password\":\"pw\",\"firstName\":\"a\",\"lastName\":\"b\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value("Only root admin can register new admins"));

        verify(adminRepo).findByUsername("admin1");
        verify(service, never()).register(any(), any(), any());
    }

    @Test
    void register_shouldReturn400_whenServiceOkFalse() throws Exception {
        Admin caller = new Admin();
        caller.setUsername("root");
        caller.setRoot(true);

        when(adminRepo.findByUsername("root")).thenReturn(Optional.of(caller));
        when(service.register(any(), any(), any())).thenReturn(Map.of("ok", false, "message", "bad"));

        mockMvc.perform(post("/api/verify/register")
                        .cookie(new Cookie("adminUsername", "root"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new\",\"password\":\"pw\",\"firstName\":\"a\",\"lastName\":\"b\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(adminRepo).findByUsername("root");
        verify(service).register(any(), any(), any());
    }

    @Test
    void register_shouldReturn200_whenServiceOkTrue() throws Exception {
        Admin caller = new Admin();
        caller.setUsername("root");
        caller.setRoot(true);

        when(adminRepo.findByUsername("root")).thenReturn(Optional.of(caller));
        when(service.register(any(), any(), any())).thenReturn(Map.of("ok", true, "username", "new"));

        mockMvc.perform(post("/api/verify/register")
                        .cookie(new Cookie("adminUsername", "root"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new\",\"password\":\"pw\",\"firstName\":\"a\",\"lastName\":\"b\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.username").value("new"));

        verify(adminRepo).findByUsername("root");
        verify(service).register(any(), any(), any());
    }
}
