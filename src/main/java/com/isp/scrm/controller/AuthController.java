package com.isp.scrm.controller;

import com.isp.scrm.dto.LoginRequest;
import com.isp.scrm.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        log.info("Login attempt for user={}", request.getUsername());
        // MOCK (dev only)
        if (!"user".equals(request.getUsername()) || !"password".equals(request.getPassword())) {
            return ResponseEntity.status(401).build();
        }

        String token = jwtService.generateToken(request.getUsername());
        log.info("Login attempt for user={}", request.getUsername());

        return ResponseEntity.ok(token);
    }
}