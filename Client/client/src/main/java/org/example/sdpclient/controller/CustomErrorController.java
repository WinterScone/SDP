package org.example.sdpclient.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<?> handleError(HttpServletRequest request, HttpServletResponse response) {
        String accept = request.getHeader("Accept");
        int status = response.getStatus();

        String uri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        if (uri != null && uri.startsWith("/api/")) {
            return ResponseEntity.status(status)
                    .body(Map.of("ok", false, "message", "Server error (" + status + ")"));
        }

        return ResponseEntity.status(302)
                .header("Location", "/404.html")
                .build();
    }
}
