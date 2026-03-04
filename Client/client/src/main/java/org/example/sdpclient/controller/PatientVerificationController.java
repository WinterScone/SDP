package org.example.sdpclient.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sdpclient.dto.PatientLogin;
import org.example.sdpclient.dto.PatientSignup;
import org.example.sdpclient.service.PatientLoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/patient")
public class PatientVerificationController {

    private final PatientLoginService service;

    public PatientVerificationController(PatientLoginService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody PatientLogin req, HttpServletResponse response) {
        var res = service.login(req);

        if (!Boolean.TRUE.equals(res.get("ok"))) {
            return ResponseEntity.status(400).body(res);
        }

        Cookie patientIdCookie = new Cookie("patientId", String.valueOf(res.get("patientId")));
        patientIdCookie.setHttpOnly(true);
        patientIdCookie.setPath("/");
        response.addCookie(patientIdCookie);

        return ResponseEntity.ok(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie patientIdCookie = new Cookie("patientId", "");
        patientIdCookie.setHttpOnly(true);
        patientIdCookie.setPath("/");
        patientIdCookie.setMaxAge(0);
        response.addCookie(patientIdCookie);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody PatientSignup req) {
        var res = service.signup(req);

        if (!Boolean.TRUE.equals(res.get("ok"))) {
            if ("Username already taken".equals(res.get("error"))) {
                return ResponseEntity.status(400)
                        .body(res);
            }
            return ResponseEntity.badRequest()
                    .body(res);
        }
        return ResponseEntity.ok(res);
    }
}



