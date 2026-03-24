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
    private final ActivityLogService activityLogService;

    public AdminLoginService(AdminRepository repo, PasswordEncoder encoder,
                            ActivityLogService activityLogService) {
        this.repo = repo;
        this.encoder = encoder;
        this.activityLogService = activityLogService;
    }

    public Map<String, Object> login(AdminLogin req) {
        var userOptional = repo.findByUsernameIgnoreCase(req.getUsername());

        if (userOptional.isEmpty()) {
            activityLogService.logAdminLoginFailed(req.getUsername());
            return Map.of("ok", false);
        }

        var user = userOptional.get();
        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            activityLogService.logAdminLoginFailed(req.getUsername());
            return Map.of("ok", false);
        }

        activityLogService.logAdminLogin(user.getId(), user.getUsername());

        return Map.of(
                "ok", true,
                "id", user.getId(),
                "username", user.getUsername(),
                "root", user.isRoot()
        );
    }

    public Map<String, Object> register(AdminRegisterRequest req, Long creatorAdminId,
                                       String creatorAdminUsername) {
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

        if (repo.findByUsernameIgnoreCase(username).isPresent()) {
            return Map.of(
                    "ok", false,
                    "message", "Username already exists"
            );
        }

        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return Map.of("ok", false, "message", "Invalid email format");
        }

        String phoneVal = req.getPhone().replaceAll("\\s+", "");
        if (!phoneVal.matches("^0\\d{10}$")) {
            return Map.of("ok", false, "message", "Phone must be a valid UK number (11 digits starting with 0)");
        }

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPasswordHash(encoder.encode(req.getPassword()));
        admin.setFirstName(req.getFirstName().trim());
        admin.setLastName(req.getLastName().trim());
        admin.setEmail(email);
        admin.setPhone(req.getPhone().replaceAll("\\s+", ""));

        repo.save(admin);

        // Log the activity
        String newAdminFullName = admin.getFirstName() + " " + admin.getLastName();
        activityLogService.logAdminCreated(
                admin.getUsername(),
                newAdminFullName,
                creatorAdminId,
                creatorAdminUsername
        );

        return Map.of(
                "ok", true,
                "username", admin.getUsername()
        );
    }
}

