package org.example.sdpclient.service;

import org.example.sdpclient.dto.AdminLogin;
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
                "username",
                user.getUsername()
        );
    }

    public Map<String, Object> register(AdminLogin req) {
        if (repo.findByUsername(req.getUsername()).isPresent()) {
            return Map.of(
                    "ok",
                    false,
                    "message",
                    "Username already exists"
            );
        }

        Admin admin = new Admin(
                null,
                req.getUsername(),
                encoder.encode(req.getPassword())
        );

        repo.save(admin);

        return Map.of(
                "ok",
                true,
                "username",
                admin.getUsername()
        );
    }
}

