package org.example.sdpclient.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestPingController {

    @GetMapping("/api/admin/ping")
    public String adminPing() {
        return "pong";
    }

    @GetMapping("/api/public/ping")
    public String publicPing() {
        return "pong-public";
    }
}
