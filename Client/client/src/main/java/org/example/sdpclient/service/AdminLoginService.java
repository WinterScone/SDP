package org.example.sdpclient.service;

import org.example.sdpclient.dto.AdminLogin;
import org.example.sdpclient.dto.AdminRegisterRequest;
import org.example.sdpclient.entity.Admin;
import org.example.sdpclient.repository.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
public class AdminLoginService {

    private final AdminRepository repo;
    private final PasswordEncoder encoder;

    public AdminLoginService(AdminRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public Map<String, Object> login(AdminLogin req) {
        var userOptional = repo.findByUsername(req.getUsername());

        if (userOptional.isEmpty()) {
            return Map.of("ok", false);
        }

        var user = userOptional.get();
        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            return Map.of("ok", false);
        }

        return Map.of(
                "ok", true,
                "username", user.getUsername(),
                "root", user.isRoot()
        );
    }

    public Map<String, Object> register(AdminRegisterRequest req) {
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()
                || req.getPassword() == null || req.getPassword().isEmpty()
                || req.getFirstName() == null || req.getFirstName().trim().isEmpty()
                || req.getLastName() == null || req.getLastName().trim().isEmpty()
                || req.getEmail() == null || req.getEmail().trim().isEmpty()
                || req.getPhone() == null || req.getPhone().trim().isEmpty()) {
            return Map.of(
                    "ok", false,
                    "message", "All fields are required"
            );
        }

        var username = req.getUsername().trim();
        var email = req.getEmail().trim();

        if (repo.findByUsername(username).isPresent()) {
            return Map.of(
                    "ok", false,
                    "message", "Username already exists"
            );
        }

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPasswordHash(encoder.encode(req.getPassword()));
        admin.setFirstName(req.getFirstName().trim());
        admin.setLastName(req.getLastName().trim());
        admin.setEmail(email);
        admin.setPhone(req.getPhone().trim());

        repo.save(admin);

        return Map.of(
                "ok", true,
                "username", admin.getUsername()
        );
    }
}

