package com.bookmind.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
public class PayOSConfig {

    private static final Logger log = LoggerFactory.getLogger(PayOSConfig.class);

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        if (clientId == null || clientId.isBlank() || clientId.contains("your_client_id")) {
            log.warn("PayOS credentials not configured yet in application.properties. PayOS will run in fallback VietQR mode.");
            return null;
        }
        try {
            log.info("Initializing PayOS client with Client ID: {}...", clientId.substring(0, Math.min(clientId.length(), 6)));
            return new PayOS(clientId.trim(), apiKey.trim(), checksumKey.trim());
        } catch (Exception e) {
            log.error("Failed to initialize PayOS client: {}", e.getMessage());
            return null;
        }
    }
}
