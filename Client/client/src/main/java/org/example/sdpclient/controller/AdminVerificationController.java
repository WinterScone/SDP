package org.example.sdpclient.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.example.sdpclient.dto.AdminLogin;
import org.example.sdpclient.dto.AdminRegisterRequest;
import org.example.sdpclient.service.AdminLoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity<?> login(@RequestBody AdminLogin req, HttpServletResponse response) {
        var res = service.login(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) {
            return ResponseEntity.status(400).body(res);
        }

        Cookie usernameCookie = new Cookie("adminUsername", (String) res.get("username"));
        usernameCookie.setHttpOnly(true);
        usernameCookie.setPath("/");
        response.addCookie(usernameCookie);

        Cookie rootCookie = new Cookie("adminRoot", String.valueOf(res.get("root")));
        rootCookie.setPath("/");
        response.addCookie(rootCookie);

        return ResponseEntity.ok(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie usernameCookie = new Cookie("adminUsername", "");
        usernameCookie.setHttpOnly(true);
        usernameCookie.setPath("/");
        usernameCookie.setMaxAge(0);
        response.addCookie(usernameCookie);

        Cookie rootCookie = new Cookie("adminRoot", "");
        rootCookie.setPath("/");
        rootCookie.setMaxAge(0);
        response.addCookie(rootCookie);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        String username = getCookieValue(request, "adminUsername");
        if (username == null || username.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Unauthorized"));
        }
        return ResponseEntity.ok(Map.of("ok", true, "username", username));
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AdminRegisterRequest req) {
        var res = service.register(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) {
            return ResponseEntity.status(400).body(res);
        }
        return ResponseEntity.ok(res);
    }
}

