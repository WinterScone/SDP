package org.example.sdpclient.service;

import org.example.sdpclient.dto.PatientLogin;
import org.example.sdpclient.dto.PatientSignup;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.repository.PatientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PatientLoginService {

    private final PatientRepository repo;
    private final PasswordEncoder encoder;

    public PatientLoginService(PatientRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public Map<String, Object> login(PatientLogin req) {
        var userOptional = repo.findByUsername(req.getUsername());
        if (userOptional.isEmpty()) {
            return Map.of(
                    "ok",
                    false
            );
        }

        var user = userOptional.get();
        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            return Map.of(
                    "ok",
                    false
            );
        }

        return Map.of(
                "ok",
                true,
                "username", user.getUsername(),
                "patientId", user.getId()
        );
    }

    public Map<String, Object> signup(PatientSignup req) {

        if (req.getUsername() == null || req.getUsername().isBlank() || req.getPassword() == null
                || req.getPassword().isBlank() || req.getFirstName() == null || req.getFirstName().isBlank()
                || req.getLastName() == null || req.getLastName().isBlank() || req.getDateOfBirth() == null
                || req.getDateOfBirth().isBlank()) {
            return Map.of("ok",
                    false,
                    "error",
                    "Missing required fields"
            );
        }

        String username = req.getUsername().trim();

        if (repo.existsByUsername(username)) {
            return Map.of(
                    "ok",
                    false,
                    "error",
                    "Username already taken"
            );
        }

        Patient patient = new Patient();
        patient.setUsername(username);
        patient.setPasswordHash(encoder.encode(req.getPassword()));

        patient.setFirstName(req.getFirstName().trim());
        patient.setLastName(req.getLastName().trim());
        patient.setDateOfBirth(req.getDateOfBirth().trim());

        if (req.getEmail() == null || req.getEmail().isBlank()) {
            patient.setEmail(null);
        } else {
            patient.setEmail(req.getEmail().trim());
        }

        if (req.getPhone() == null || req.getPhone().isBlank()) {
            patient.setPhone(null);
        } else {
            patient.setPhone(req.getPhone().trim());
        }

        patient.setLinkedAdminId(null);
        patient.setLinkedAdminName(null);

        Patient saved = repo.save(patient);

        return Map.of(
                "ok",
                true,
                "id", saved.getId(),
                "username", saved.getUsername()
        );
    }
}


