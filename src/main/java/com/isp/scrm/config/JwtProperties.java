package com.isp.scrm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
@Data
public class JwtProperties {
    private String secret;
    private String issuer;
    private String subject;
    private String type;
    private long expirationDays;
}

