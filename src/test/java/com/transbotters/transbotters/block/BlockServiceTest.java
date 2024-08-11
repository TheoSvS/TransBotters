package com.transbotters.transbotters.block;
//
import com.transbotters.transbotters.ETransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.IOException;
import java.math.BigInteger;

@SpringBootTest
public class BlockServiceTest {

    @Autowired
    BlockService blockService;

    //TOKEN SAST
    //https://etherscan.io/token/0x3136017b753f41d63c96d19b976fb6f76a6e282a
    //sample transactions
    BigInteger SAST_TOKEN_CREATION_BLOCK = BigInteger.valueOf(20496652);
    BigInteger SAST_TOKEN_BUY_BLOCK = BigInteger.valueOf(20496662); //0.0₁₁7087  0x4f0191bd795e4a7966574fc2362457dc132f867a711ac89d10edd39412cadeb1
    BigInteger SAST_TOKEN_REMOVE_LIQUIDITY_BLOCK = BigInteger.valueOf(20496965);
    BigInteger SAST_TOKEN_STOP_BLOCK= BigInteger.valueOf(20496662);


    @Test
    void testReadCreateTokenTransactionsOnBlock(){
        blockService.readBlockTransactionByType(SAST_TOKEN_CREATION_BLOCK, ETransactionType.TOKEN_CREATION);
    }

    @Test
    void testReadBuyTokenInBlock()  {
        blockService.readBlockTransactionByType(SAST_TOKEN_BUY_BLOCK, ETransactionType.TOKEN_BOUGHT);
    }

    @Test
    void testReadAddLiquidityInBlock() throws IOException {
        blockService.readBlockTransactionByType(SAST_TOKEN_CREATION_BLOCK, ETransactionType.ADD_LIQUIDITY);
    }

    @Test
    void testReadRemoveLiquidityInBlock() throws IOException {
        blockService.readBlockTransactionByType(SAST_TOKEN_REMOVE_LIQUIDITY_BLOCK, ETransactionType.REMOVE_LIQUIDITY);
    }

    @Test
    void testReadBuyBlocksRangedByType() throws IOException{
        blockService.readTransactionByBlockRange(SAST_TOKEN_CREATION_BLOCK, SAST_TOKEN_STOP_BLOCK, ETransactionType.TOKEN_BOUGHT);
    }
}
