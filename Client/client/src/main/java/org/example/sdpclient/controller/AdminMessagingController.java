package org.example.sdpclient.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.sdpclient.dto.SendMessageRequest;
import org.example.sdpclient.service.AdminMessagingService;
import org.example.sdpclient.service.AdminMessagingService.RecipientDto;
import org.example.sdpclient.service.AdminMessagingService.SendResult;
import org.example.sdpclient.util.CookieUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/messaging")
public class AdminMessagingController {

    private final AdminMessagingService messagingService;

    public AdminMessagingController(AdminMessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @GetMapping("/recipients")
    public ResponseEntity<List<RecipientDto>> getRecipients(HttpServletRequest request) {
        requireRoot(request);
        Long adminId = CookieUtils.getAdminIdFromCookie(request);
        return ResponseEntity.ok(messagingService.getAllRecipients(adminId));
    }

    @PostMapping("/send")
    public ResponseEntity<SendResult> sendMessage(@RequestBody SendMessageRequest body,
                                                   HttpServletRequest request) {
        requireRoot(request);

        if (body.getRecipients() == null || body.getRecipients().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No recipients selected");
        }
        if (body.getMessage() == null || body.getMessage().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }

        Long adminId = CookieUtils.getAdminIdFromCookie(request);
        String username = CookieUtils.getCookieValue(request, "adminUsername");

        SendResult result = messagingService.sendMessage(adminId, username, body.getRecipients(), body.getMessage());
        return ResponseEntity.ok(result);
    }

    private void requireRoot(HttpServletRequest request) {
        if (!CookieUtils.isRootAdmin(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only root admin can send messages");
        }
    }
}
