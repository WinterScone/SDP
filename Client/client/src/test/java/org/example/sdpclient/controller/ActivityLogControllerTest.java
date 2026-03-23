package org.example.sdpclient.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.Cookie;
import org.example.sdpclient.configuration.WebMvcConfig;
import org.example.sdpclient.dto.ActivityLogDto;
import org.example.sdpclient.service.ActivityLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ActivityLogController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = WebMvcConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class ActivityLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityLogService activityLogService;

    @Test
    void getAllActivityLogs_shouldReturn200_whenRootAdmin() throws Exception {
        ActivityLogDto dto = new ActivityLogDto(
                1L, "MEDICINE_STOCK_CHANGE", "Test", 1L, "admin", null, null, null, null, LocalDateTime.now());
        when(activityLogService.getAllLogs()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/activity-logs")
                        .cookie(new Cookie("adminRoot", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(activityLogService).getAllLogs();
    }

    @Test
    void getAllActivityLogs_shouldReturn403_whenNotRootAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/activity-logs")
                        .cookie(new Cookie("adminRoot", "false")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(activityLogService);
    }
}
