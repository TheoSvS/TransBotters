package com.transbotters.transbotters.contractcalls;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.web3j.abi.datatypes.Function;

import java.io.IOException;

@SpringBootTest
public class TokenSwapTest {
    @Autowired
    private TokenSwap tokenSwap;
    @Test
    void simulateSwapExactETHForTokensTx() throws IOException {
        // EXAMPLE Token address to receive (e.g., DAI)
        String tokenBuyAddr = "0x6b175474e89094c44da98b954eedeac495271d0f";
        Function swapExactETHForTokensFunc = tokenSwap.getSwapExactETHFunction(tokenBuyAddr);
        //boolean res = tokenSwap.isSuccessfulTxSimulation(swapExactETHForTokensFunc);
        tokenSwap.swapExactETHForTokens(tokenBuyAddr);
    }
}
