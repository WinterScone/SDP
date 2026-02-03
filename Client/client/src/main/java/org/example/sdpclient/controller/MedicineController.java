package org.example.sdpclient.controller;

import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
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
    public ResponseEntity<?> updateQuantity(@PathVariable MedicineType id, @RequestBody Map<String, Integer> body) {

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

        service.updateQuantity(id, quantity);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}



