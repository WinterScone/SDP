package org.example.sdpclient.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.sdpclient.dto.ActivityLogDto;
import org.example.sdpclient.dto.AdminDto;
import org.example.sdpclient.service.ActivityLogService;
import org.example.sdpclient.service.AdminListService;
import org.example.sdpclient.service.DatabaseResetService;
import org.example.sdpclient.service.SmsService;
import org.example.sdpclient.util.CookieUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminListService adminListService;
    private final DatabaseResetService resetService;
    private final ActivityLogService activityLogService;
    private final SmsService smsService;

    public AdminController(AdminListService adminListService,
                           DatabaseResetService resetService,
                           ActivityLogService activityLogService,
                           SmsService smsService) {
        this.adminListService = adminListService;
        this.resetService = resetService;
        this.activityLogService = activityLogService;
        this.smsService = smsService;
    }

    @GetMapping("/admins")
    public ResponseEntity<List<AdminDto>> getAllAdmins() {
        return ResponseEntity.ok(adminListService.getAllAdmins());
    }

    @PostMapping("/reset-database")
    public ResponseEntity<?> resetDatabase(HttpServletRequest request) {
        if (!CookieUtils.isRootAdmin(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only root admin can reset the database");
        }

        try {
            var stats = resetService.getResetStats();
            Long adminId = CookieUtils.getAdminIdFromCookie(request);
            String adminUsername = CookieUtils.getCookieValue(request, "adminUsername");

            resetService.resetToSeedData(adminId, adminUsername);

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "message", String.format("Deleted %d admins, %d patients, %d prescriptions. Seed data preserved.",
                            stats.adminsToDelete(),
                            stats.patientsToDelete(),
                            stats.prescriptionsToDelete())
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "ok", false,
                            "message", "Failed to reset database: " + e.getMessage()
                    ));
        }
    }

    @GetMapping("/activity-logs")
    public ResponseEntity<List<ActivityLogDto>> getAllActivityLogs(HttpServletRequest request) {
        if (!CookieUtils.isRootAdmin(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only root admin can access activity logs");
        }

        List<ActivityLogDto> logs = activityLogService.getAllLogs();
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/sms/test")
    public ResponseEntity<?> sendTestSms(@RequestBody Map<String, String> req) {
        String to = req.get("to");
        String message = req.get("message");

        if (to == null || to.isBlank() || message == null || message.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "error", "Both 'to' and 'message' are required"));
        }

        String error = smsService.sendSms(to, message);

        if (error == null) {
            return ResponseEntity.ok(Map.of("ok", true, "message", "SMS sent to " + to));
        } else {
            return ResponseEntity.status(500)
                    .body(Map.of("ok", false, "error", error));
        }
    }
}
