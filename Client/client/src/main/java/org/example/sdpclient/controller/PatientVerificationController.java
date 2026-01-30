package org.example.sdpclient.controller;



import org.example.sdpclient.dto.PatientLogin;
import org.example.sdpclient.repository.PatientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/patient")
public class PatientVerificationController {

    private final PatientRepository repo;
    private final PasswordEncoder encoder;

    public PatientVerificationController(PatientRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody PatientLogin req) {

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
}

