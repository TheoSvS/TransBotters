package com.transbotters.transbotters.token;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.util.MapUtils;
import org.web3j.utils.Numeric;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

@SpringBootTest
@Slf4j
public class TokenServiceTest {

     @Autowired
     TokenService tokenService;

    //FinBridge (FNBD)
    final String FIN_BRIDGE_CREATION_TX_FROM_ADDRESS =  "0x19aa685ba44e41ad126c8d49de652ee992c3a223";
    final BigInteger FIN_BRIDGE_CREATION_TX_NONCE = Numeric.decodeQuantity("0x0");
    final String FNBD_TOKEN_ADDRESS = "0x9ea0b24d9bde82edb734b827d34a229224e3d41b";
    final String FNBD_UNISWAP_V2_MAINET_PAIR = "0x3836986571e3f8d15b1abbf0d31f43bb10b40667";

    @Test
    void testReadCreateTokenTransactionsOnBlock(){
        String tokenAddress = tokenService.computeTokenAddress(FIN_BRIDGE_CREATION_TX_FROM_ADDRESS, FIN_BRIDGE_CREATION_TX_NONCE);
        Assertions.assertNotNull(tokenAddress);
        log.info("https://etherscan.io/token/{}",tokenAddress);
        if(Objects.nonNull(tokenAddress)){
            String tokenName = tokenService.getName(tokenAddress);
            String tokenSymbol = tokenService.getSymbol(tokenAddress);
            Assertions.assertNotNull(tokenName);
            Assertions.assertNotNull(tokenSymbol);
            log.info("Token: {} ({})",tokenName, tokenSymbol);
        }
    }

    @Test
    void findAllEthPairs(){
        Map<String, String> pairs = tokenService.finalAllEthPairs(FNBD_TOKEN_ADDRESS);
        if(MapUtils.isEmpty(pairs)){
            log.info("No pairs found");
        } else{
            log.info("Pairs: {}", pairs);
        }
    }

    @Test
    void readLiquidityEth(){
     this.tokenService.readLiquidityEth(FNBD_UNISWAP_V2_MAINET_PAIR);
    }
}