package org.example.sdpclient.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.example.sdpclient.dto.AdminLogin;
import org.example.sdpclient.dto.AdminRegisterRequest;
import org.example.sdpclient.repository.AdminRepository;
import org.example.sdpclient.service.AdminLoginService;
import org.example.sdpclient.util.CookieUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth/admins")
public class AdminAuthController {

    private final AdminLoginService service;
    private final AdminRepository adminRepo;

    public AdminAuthController(AdminLoginService service, AdminRepository adminRepo) {
        this.service = service;
        this.adminRepo = adminRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLogin req, HttpServletResponse response) {
        var res = service.login(req);
        if (!Boolean.TRUE.equals(res.get("ok"))) {
            return ResponseEntity.status(400).body(res);
        }

        Cookie idCookie = new Cookie("adminId", String.valueOf(res.get("id")));
        idCookie.setHttpOnly(true);
        idCookie.setPath("/");
        response.addCookie(idCookie);

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
        Cookie idCookie = new Cookie("adminId", "");
        idCookie.setHttpOnly(true);
        idCookie.setPath("/");
        idCookie.setMaxAge(0);
        response.addCookie(idCookie);

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
        String username = CookieUtils.getCookieValue(request, "adminUsername");
        if (username == null || username.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Unauthorized"));
        }
        return ResponseEntity.ok(Map.of("ok", true, "username", username));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(HttpServletRequest request, @RequestBody AdminRegisterRequest req) {
        String username = CookieUtils.getCookieValue(request, "adminUsername");
        if (username == null || username.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Unauthorized"));
        }

        var caller = adminRepo.findByUsername(username);
        if (caller.isEmpty() || !caller.get().isRoot()) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", "Only root admin can register new admins"));
        }

        Long creatorAdminId = caller.get().getId();
        String creatorAdminUsername = caller.get().getUsername();

        var res = service.register(req, creatorAdminId, creatorAdminUsername);
        if (!Boolean.TRUE.equals(res.get("ok"))) {
            return ResponseEntity.status(400).body(res);
        }
        return ResponseEntity.ok(res);
    }
}
