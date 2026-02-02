package org.example.sdpclient.controller;

import org.example.sdpclient.dto.AdminLogin;
import org.example.sdpclient.service.AdminLoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/verify")
public class AdminVerificationController {

    private final AdminLoginService service;

    public AdminVerificationController(AdminLoginService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLogin req) {
        var res = service.login(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) {
            return ResponseEntity.status(400)
                    .body(res);
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AdminLogin req) {
        var res = service.register(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) {
            return ResponseEntity.status(400)
                    .body(res);
        }
        return ResponseEntity.ok(res);
    }
}

