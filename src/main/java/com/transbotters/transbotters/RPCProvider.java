package com.transbotters.transbotters;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
@Component
@Slf4j
public class RPCProvider {
    private String INFURA_URL;
    private String ALCHEMY_URL;

    @PostConstruct
    private void readProperties() {
        Properties prop = new Properties();

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("chain_infra_api_url.properties")) {
            if (input == null) {
                log.error("Unable to find chain_infra_api_url.properties");
                return;
            }
            // Load the properties file
            prop.load(input);

            // Get property values
            INFURA_URL = prop.getProperty("eth_node_infra_provider_key");
            ALCHEMY_URL = prop.getProperty("alchemy_node_infra_provider_key");

            // Log property values
            log.info("Infura URL: {}", INFURA_URL);
            log.info("Alchemy URL: {}", ALCHEMY_URL);
        } catch (IOException ex) {
            log.error("Error loading properties file: {}", ex.getMessage(), ex);
        }
    }
}