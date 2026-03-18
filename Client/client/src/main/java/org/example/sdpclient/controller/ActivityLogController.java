package org.example.sdpclient.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.example.sdpclient.dto.ActivityLogDto;
import org.example.sdpclient.service.ActivityLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/activity-logs")
public class ActivityLogController {

    private final ActivityLogService service;

    public ActivityLogController(ActivityLogService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ActivityLogDto>> getAllActivityLogs(HttpServletRequest request) {
        // Only root admin can view activity logs
        if (!isRootAdmin(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only root admin can access activity logs");
        }

        List<ActivityLogDto> logs = service.getAllLogs();
        return ResponseEntity.ok(logs);
    }

    private boolean isRootAdmin(HttpServletRequest request) {
        String rootStr = getCookieValue(request, "adminRoot");
        return "true".equalsIgnoreCase(rootStr);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }
}
