package org.example.sdpclient.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sdpclient.dto.ReduceMedicineRequest;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.example.sdpclient.service.MedicineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService service;

    public MedicineController(MedicineService service) {
        this.service = service;
    }

    @GetMapping
    public List<Medicine> getAllMedicines() {
        return service.getAll();
    }

    @PatchMapping("/{id}/quantity")
    public ResponseEntity<?> updateQuantity(@PathVariable MedicineType id, @RequestBody Map<String, Integer> body,
                                           HttpServletRequest request) {

        Integer quantity = body.get("quantity");
        if (quantity == null || quantity < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "ok",
                            false,
                            "message",
                            "Quantity must be >= 0")
                    );
        }

        if (!service.exists(id)) {
            return ResponseEntity.status(400)
                    .body(Map.of(
                            "ok",
                            false,
                            "message",
                            "Not found")
                    );
        }

        Long adminId = getAdminIdFromCookie(request);
        String adminUsername = getCookieValue(request, "adminUsername");

        service.updateQuantity(id, quantity, adminId, adminUsername);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/reduce")
    public ResponseEntity<?> reduceMedicine(@RequestBody ReduceMedicineRequest request) {

        if (request.getMedicineName() == null || request.getQuantity() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "message", "Medicine name and quantity required"));
        }

        try {
            service.reduceQuantityByName(
                    request.getMedicineName(),
                    request.getQuantity()
            );
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "message", ex.getMessage()));
        }
    }

    private Long getAdminIdFromCookie(HttpServletRequest request) {
        String idStr = getCookieValue(request, "adminId");
        if (idStr == null || idStr.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
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



