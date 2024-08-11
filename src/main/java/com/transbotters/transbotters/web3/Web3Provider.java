package com.transbotters.transbotters.web3;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * Service that provides Web 3 connection with alchemy/infura provider
 */
@ApplicationScope
@PropertySource("classpath:chain_infra_api_url.properties")
@Component
public class Web3Provider {

    @Value("${eth_node_infra_provider_key}")
    private String INFURA_URL;
    @Value("${alchemy_node_infra_provider_key}")
    private String ALCHEMY_URL;

    @Getter
    private Web3j web3j;

    @PostConstruct
    public void Setup() {
        web3j = Web3j.build(new HttpService(ALCHEMY_URL));
    }

    @PreDestroy
    public void cleanup() {
        this.web3j.shutdown();
    }
}
