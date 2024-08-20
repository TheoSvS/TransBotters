package com.transbotters.transbotters.web3;

import com.transbotters.transbotters.Utils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * Service that provides Web 3 connection with alchemy/infura provider
 */
@ApplicationScope
@Component
public class Web3Provider {

    @Value("${infura_eth_rpc_key}")
    private String INFURA_URL;

    @Value("${alchemy_eth_rpc_key}")
    private String ALCHEMY_URL;

    @Getter
    @Value("${chain_explorer}")
    private String chainExplorerURL;

    @Getter
    @Value("${eth_uniswapV2_router02}")
    private String ethUniswapV2Router;

    @Getter
    @Value("${bot_wallet_addr}")
    private String botWalletAddr;

    @Getter
    @Value("${privk}")
    private String privateKey;

    @Getter
    @Value("${weth_address}")
    private String wethAddress;

    @Getter
    private Web3j web3j;

    @PostConstruct
    public void setup() {
        validateProperties();
        web3j = Web3j.build(new HttpService(ALCHEMY_URL));
    }

    public void validateProperties() {
        //TODO: validate everything else we need
        boolean missingProperties = Utils.findEmptyProperties(ALCHEMY_URL,chainExplorerURL,botWalletAddr,privateKey);
        if (missingProperties) {
            throw new IllegalStateException("Missing mandatory application properties!!");
        }
    }

    @PreDestroy
    public void cleanup() {
        this.web3j.shutdown();
    }
}
