package com.isp.scrm.controller;

import com.isp.scrm.config.RotateProperties;
import com.isp.scrm.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/internal/token")
@RequiredArgsConstructor
public class TokenAdminController {

    private final JwtService jwtService;
    private final RotateProperties rotateProps;
    private static final Logger log = LoggerFactory.getLogger(TokenAdminController.class);


    @PostMapping("/rotate")
    public Map<String, String> rotate(@RequestHeader("X-ROTATE-KEY") String rotateKey) {
        if (rotateKey == null || !rotateProps.getKey().equals(rotateKey)) {
            throw new AccessDeniedException("Invalid rotate key");
        }
        log.info("Rotate key received: x-rotate-key={}", rotateKey);
        String token = jwtService.generateTechnicalToken();
        log.info("Token generated: token={}", token);

        return Map.of(
                "token", token,
                "note", "Copy this token and update your secret. Old token becomes invalid after redeploy."
        );
    }
}