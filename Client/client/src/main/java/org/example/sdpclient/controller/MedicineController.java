package org.example.sdpclient.controller;

import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineRepository repo;

    public MedicineController(MedicineRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Medicine> getAll() {
        return repo.findAll(Sort.by("medicineId"));
    }

    @PatchMapping("/{id}/quantity")
    public ResponseEntity<?> updateQuantity(@PathVariable MedicineType id,
                                            @RequestBody Map<String, Integer> body) {

        Integer quantity = body.get("quantity");
        if (quantity == null || quantity < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "message", "Quantity must be >= 0"));
        }

        Medicine med = repo.findById(id).orElse(null);
        if (med == null) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Not found"));
        }

        med.setQuantity(quantity);
        repo.save(med);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}


