package org.example.sdpclient.configuration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.example.sdpclient.controller.TestPingController;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TestPingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, AdminAuthenticationInterceptor.class})
class AdminAuthenticationInterceptorMvcConfigTest {

    @Autowired MockMvc mockMvc;

    @Test
    void blocks_adminPath_withoutCookie() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("{\"ok\":false,\"message\":\"Unauthorized\"}"));
    }

    @Test
    void allows_adminPath_withCookie() throws Exception {
        mockMvc.perform(get("/api/admin/ping")
                        .cookie(new Cookie("adminUsername", "alice")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    void doesNotBlock_nonAdminPath_evenWithoutCookie() throws Exception {
        mockMvc.perform(get("/api/public/ping"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("pong-public"));
    }
}
