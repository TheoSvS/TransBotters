package com.transbotters.transbotters.web3;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class Web3ProviderTest {

    @Autowired
    private Web3Provider web3Provider;

    @Test
    public void testProperties(){
        Assertions.assertNotNull(web3Provider.getWeb3j());
    }
}
