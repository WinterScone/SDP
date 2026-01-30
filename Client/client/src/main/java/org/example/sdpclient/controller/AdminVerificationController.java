package org.example.sdpclient.controller;

import org.example.sdpclient.dto.AdminLogin;
import org.example.sdpclient.entity.Admin;
import org.example.sdpclient.repository.AdminRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/verify")
public class AdminVerificationController {

    private final AdminRepository repo;
    private final PasswordEncoder encoder;

    public AdminVerificationController(AdminRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLogin req) {
        var userOpt = repo.findByUsername(req.getUsername());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("ok", false));
        }

        var user = userOpt.get();
        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("ok", false));
        }

        return ResponseEntity.ok(Map.of("ok", true, "username", user.getUsername()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AdminLogin req) {

        if (repo.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity
                    .status(409)
                    .body(Map.of("ok", false, "message", "Username already exists"));
        }

        Admin admin = new Admin(
                null,
                req.getUsername(),
                encoder.encode(req.getPassword())
        );

        repo.save(admin);

        return ResponseEntity.ok(
                Map.of("ok", true, "username", admin.getUsername())
        );
    }

}
