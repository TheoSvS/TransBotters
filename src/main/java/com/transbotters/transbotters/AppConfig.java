package com.transbotters.transbotters;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;

@Configuration
@ConfigurationProperties
@Data
public class AppConfig {
    private String appTitle;

    @Bean
    public CustomLoggingFilter logFilter() {
        CustomLoggingFilter filter = new CustomLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(false); // You can set this to true to include headers as well
        filter.setBeforeMessagePrefix("Incoming Request: ");
        filter.setAfterMessageSuffix(" [END]");
        return filter;
    }

    @Bean
    public AuthenticationManager noopAuthenticationManager() {
        return authentication -> {
            throw new AuthenticationServiceException("No user authentication required");
        };
    }

}