package com.isp.scrm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "security.rotate")
@Getter
@Setter
public class RotateProperties {
    private String key;
}
