package com.transbotters.transbotters.token;

import com.transbotters.transbotters.web3.Web3Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint112;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint80;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.utils.Numeric;

@Component
@Slf4j
public class TokenService {

    private Web3Provider web3Provider;

    TokenService(Web3Provider web3Provider){
        this.web3Provider = web3Provider;
    }

    //TODO check which dexes are needed
    //    Most V2-style factories use getPair(address,address) → returns pool address.
    //    V3-style (UniswapV3, PancakeV3) use getPool(address,address,uint24 fee).
    private final Map<String, String> DEX_FACTORIES = Map.ofEntries(
            // Top-tier DEXes
            Map.entry("UniswapV2_Mainnet", "0x5C69bEe701ef814a2B6a3EDD4B1652CB9cc5aA6f"),
            Map.entry("UniswapV3_Mainnet", "0x1F98431c8aD98523631AE4a59f267346ea31F984"),
            Map.entry("SushiSwap_Mainnet", "0xC0AEe478e3658e2610c5F7A4A2E1777CE9e4f2Ac"),
            Map.entry("PancakeSwap_BSC", "0xBCfCcbde45cE874adCB698cC183deBcF17952812"),
            Map.entry("PancakeSwapV3_BSC", "0x1097053Fd2ea711dad45caCcc45EfF7548fCB362"),
            Map.entry("ShibaSwap_Mainnet", "0x115934131916C8b277DD010Ee02de363c09d037c"),

            // Polygon / Base / Optimism / Arbitrum
            Map.entry("QuickSwap_Polygon", "0x5757371414417b8c6caad45baef941abc7d3ab32"),
            Map.entry("UniswapV3_Polygon", "0x1F98431c8aD98523631AE4a59f267346ea31F984"),
            Map.entry("UniswapV3_Optimism", "0x1F98431c8aD98523631AE4a59f267346ea31F984"),
            Map.entry("UniswapV3_Arbitrum", "0x1F98431c8aD98523631AE4a59f267346ea31F984"),
            Map.entry("UniswapV3_Base", "0x33128a8fc17869897dcE68Ed026d694621f6FDfD"),
            Map.entry("BaseSwap_Base", "0x9Ad6C38BE9428d9A0E2a52f7d08E34BcAA60D9d9"),
            // BSC ecosystem
            Map.entry("ApeSwap_BSC", "0x0841BD0B734E4F5853f0dD8d7EA041c241fb0Da6"),
            Map.entry("Biswap_BSC", "0x858E3312ed3A876947EA49d572A7C42DE08af7EE"),
            Map.entry("MDEX_BSC", "0x3CD1C270D2D8E422e563A1E8eDd2E379b44D50f0"),
            Map.entry("BabySwap_BSC", "0xBbfCcbbeeb066C8B162c5275Bf273F2BFEa9900b"),
            Map.entry("Thena_BSC", "0xBcFcC37d9E2C1DdACd5Bd8fF0e7eDb8d321A0F3b"),
            // Ethereum forks / alt DEXes
            Map.entry("KyberSwap_Classic", "0x1c758aF0688502e49140230F6b0EBd376d429be5"),
            Map.entry("BalancerV2_Mainnet", "0x8E5698dc4897dc12243c8642e77B4f21349Db97C"),
            Map.entry("Curve_Mainnet", "0xB9fC157394Af804a3578134A6585C0dc9cc990d4"),
            Map.entry("DODO_Mainnet", "0x6A80BfDb013ED0D0Dd4375fE952EAe6D38A12E0C"),
            Map.entry("Velodrome_Optimism", "0x25CbdDb98b35ab1FF77413456B31EC81A6B6B746"),
            Map.entry("BeethovenX_Fantom", "0x20dd72Ed959b6147912C2e529F0a0C651c33c9ce"),
            Map.entry("SpiritSwap_Fantom", "0x16327E3FbDaCA3bcF7E38f5Af2599D2DDc33aE52"),
            Map.entry("SpookySwap_Fantom", "0xF491e7B69E4244ad4002BC14e878a34207E38c29"),
            Map.entry("WOOFi_Arbitrum", "0x7A5f6fE9C2bC6c00C8372b4C98bE6E8f1F82F69B"),
            // 🧱 Misc networks / forks
            Map.entry("TraderJoe_Avalanche", "0x9Ad6C38BE9428d9A0E2a52f7d08E34BcAA60D9d9"),
            Map.entry("LydiaFinance_Avalanche", "0xe0C1Bb6Df4851feEEdc3E14Bd509FEAF428f7655"),
            Map.entry("DefiKingdoms_Harmony", "0x24ad62502d1C652Cc7684081169D04896aC20f30"),
            Map.entry("VelasPad_Velas", "0xD8A2fF88b0eA6c2fa2C0A09C4B3b30aA24d3dC2E"),
            Map.entry("MoonSwap_Moonriver", "0x3Bc94E198d7A59B6C46D8FbaF0B2c72E467cC3a1"),
            Map.entry("Solarbeam_Moonriver", "0x049581aEB6Fe262727f290165C29BDAB065a1B68"),
            Map.entry("Wanswap_Wanchain", "0x2A6C48Ff9aA2f14B66F1AbeE7d6d0aaedcA7aF3E")
    );
    private final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";

    /*
     Base addresses
     */
    private final String[] BASES = {
            "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2", // WETH
            "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", // USDC
            "0xdAC17F958D2ee523a2206206994597C13D831ec7"  // USDT
    };

    String WETH_BASE = BASES[0];

    public String computeTokenAddress(String sourceAddress, BigInteger nonce){
        byte[] encoded = RlpEncoder.encode(
                new RlpList(
                        RlpString.create(Numeric.hexStringToByteArray(sourceAddress)),
                        RlpString.create(nonce)
                )
        );
        byte[] hash = Hash.sha3(encoded);
        String address = "0x" + Numeric.toHexString(hash).substring(26);
        return address.toLowerCase();
    }

    public Map<String, String> finalAllEthPairs(String tokenAddress){
        return findAllPairs(tokenAddress, BASES[0]);
    }

    public Map<String, String> findAllPairs(String tokenAddress, String baseAddress){
        Map<String,String> pairs = new HashMap<>();
        for (Map.Entry<String, String> entry : DEX_FACTORIES.entrySet()) {
            String dexName = entry.getKey();
            String dexHash = entry.getValue();
            String pair =  findPair(tokenAddress, baseAddress, dexHash, dexName);
            if(!ZERO_ADDRESS.equalsIgnoreCase(pair)){
                pairs.put(dexName, pair);
            }
        }
        return pairs;
    }

    private String findPair(
            String tokenAddress,
            String baseAddress,
            String dexHash,
            String dexName
            ) {
        Function getPairFn = new Function("getPair", Arrays.asList(new Address(tokenAddress), new Address(baseAddress)), Arrays.asList(new TypeReference<Address>() {}));
        String encodedFn = FunctionEncoder.encode(getPairFn);
            try {
                EthCall call = web3Provider.getWeb3j().ethCall(
                        Transaction.createEthCallTransaction(null, dexHash, encodedFn),
                        DefaultBlockParameterName.LATEST).send();
                String result = call.getValue();
                if(result == null || result.equals("0x")){
                    log.info("❌ No pair on " + dexName);
                    return ZERO_ADDRESS;
                }
                String pairAddress = "0x" + result.substring(26); // strip 12 bytes of prefix
                if (!pairAddress.equalsIgnoreCase(ZERO_ADDRESS)) {
                    log.info("✅ Found pair on " + dexName + ": " + pairAddress);
                    return pairAddress;
                } else {
                    log.info("❌ No pair on " + dexName);
                }
            } catch (IOException e) {
                log.error("⚠️  Error checking " + dexName + ": " + e.getMessage());
            }
        return ZERO_ADDRESS;
    }

    public void readLiquidityEth(String pairAddress){
        try {
            readLiquidity(pairAddress, getEthUsd());
        } catch (Exception e){
            log.error("Could not read liquidity of "+pairAddress);
        }
    }

    public void readLiquidity(String pairAddress, BigDecimal eth) throws Exception {
        Web3j web3 = web3Provider.getWeb3j();
        Function token0Fn = new Function(
                "token0",
                Collections.emptyList(),
                Arrays.asList(new TypeReference<Address>() {})
        );
        EthCall token0Call = web3.ethCall(
                Transaction.createEthCallTransaction(null, pairAddress, FunctionEncoder.encode(token0Fn)),
                DefaultBlockParameterName.LATEST
        ).send();
        String token0 = "0x" + token0Call.getValue().substring(26);

        Function token1Fn = new Function(
                "token1",
                Collections.emptyList(),
                Arrays.asList(new TypeReference<Address>() {})
        );
        EthCall token1Call = web3.ethCall(
                Transaction.createEthCallTransaction(null, pairAddress, FunctionEncoder.encode(token1Fn)),
                DefaultBlockParameterName.LATEST
        ).send();
        String token1 = "0x" + token1Call.getValue().substring(26);

        Function getReservesFn = new Function(
                "getReserves",
                Collections.emptyList(),
                Arrays.asList(
                        new TypeReference<Uint112>() {},
                        new TypeReference<Uint112>() {},
                        new TypeReference<Uint32>() {}
                )
        );

        EthCall reservesCall = web3.ethCall(
                Transaction.createEthCallTransaction(null, pairAddress, FunctionEncoder.encode(getReservesFn)),
                DefaultBlockParameterName.LATEST
        ).send();

        List<Type> decoded = FunctionReturnDecoder.decode(reservesCall.getValue(), getReservesFn.getOutputParameters());

        if (decoded.size() < 2) {
            System.out.println("⚠️  Could not decode reserves");
            return;
        }

        int dec0 = getDecimals(token0);
        int dec1 = getDecimals(token1);

        BigDecimal reserve0 = new BigDecimal(decoded.get(0).getValue().toString())
                .divide(BigDecimal.TEN.pow(dec0));

        BigDecimal reserve1 = new BigDecimal(decoded.get(1).getValue().toString())
                .divide(BigDecimal.TEN.pow(dec1));

        log.info("✅ Liquidity info for pair: " + pairAddress);
        boolean token0IsWETH = token0.equalsIgnoreCase(BASES[0]);
        boolean token1IsWETH = token1.equalsIgnoreCase(BASES[0]);

        if (token0IsWETH || token1IsWETH) {
            BigDecimal ethReserve = token0IsWETH ? reserve0 : reserve1;
            BigDecimal poolValueETH = ethReserve.multiply(new BigDecimal("2"));
            BigDecimal poolValueUSD = poolValueETH.multiply(eth);
            //todo fix presentations
            log.info("✅ Pool liquidity (approx):");
            log.info("  ETH reserve = " + ethReserve.toPlainString());
            log.info("  Total ≈ " + poolValueETH.toPlainString() + " ETH");
            log.info("  Total ≈ $" + poolValueUSD.toPlainString());
        } else {
            log.info("⚠️ No ETH/WETH in this pair — cannot compute value.");
        }
    }

    private int getDecimals( String token) throws Exception {
        Web3j web3 = web3Provider.getWeb3j();
        Function decimalsFn = new Function(
                "decimals",
                Collections.emptyList(),
                Arrays.asList(new TypeReference<org.web3j.abi.datatypes.generated.Uint8>() {})
        );

        EthCall decimalsCall = web3.ethCall(
                Transaction.createEthCallTransaction(null, token, FunctionEncoder.encode(decimalsFn)),
                DefaultBlockParameterName.LATEST
        ).send();

        List<Type> decoded = FunctionReturnDecoder.decode(decimalsCall.getValue(), decimalsFn.getOutputParameters());
        if (decoded.isEmpty()) return 18; // fallback
        return ((Number) decoded.get(0).getValue()).intValue();
    }

    //todo move this method to some utility. Calling eth usd could be stored in a static and get updated after dt time
    public BigDecimal getEthUsd() throws Exception {
        Web3j web3 = web3Provider.getWeb3j();
        String eth_address =  "0x5f4ec3df9cbd43714fe2740f5e3616155c5b8419";
        // latestRoundData() -> returns (uint80, int256, uint256, uint256, uint80)
        Function fn = new Function(
                "latestRoundData",
                Arrays.asList(),
                Arrays.asList(
                        new TypeReference<Uint80>() {},  // roundId
                        new TypeReference<Int256>() {},   // answer
                        new TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {}, // startedAt
                        new TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {}, // updatedAt
                        new TypeReference<Uint80>() {}    // answeredInRound
                )
        );

        EthCall response = web3.ethCall(
                Transaction.createEthCallTransaction(null, eth_address, FunctionEncoder.encode(fn)),
                DefaultBlockParameterName.LATEST
        ).send();

        List<Type> decoded = FunctionReturnDecoder.decode(response.getValue(), fn.getOutputParameters());

        if (decoded.isEmpty()) {
            throw new RuntimeException("No price returned");
        }

        BigDecimal price = new BigDecimal(decoded.get(1).getValue().toString());
        // Chainlink uses 8 decimals
        return price.divide(BigDecimal.TEN.pow(8));
    }

    public String getSymbol(String tokenAddress) {
        Web3j web3 = web3Provider.getWeb3j();
        try {
            Function symbolFn = new Function(
                    "symbol",
                    Collections.emptyList(),
                    Arrays.asList(new TypeReference<Utf8String>() {})
            );

            EthCall call = web3.ethCall(
                    Transaction.createEthCallTransaction(null, tokenAddress, FunctionEncoder.encode(symbolFn)),
                    DefaultBlockParameterName.LATEST
            ).send();

            List<Type> decoded = FunctionReturnDecoder.decode(call.getValue(), symbolFn.getOutputParameters());
            if (!decoded.isEmpty()) {
                return decoded.get(0).getValue().toString();
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error fetching symbol for " + tokenAddress + ": " + e.getMessage());
        }
        return "UNKNOWN";
    }

    public String getName(String tokenAddress) {
        Web3j web3 = web3Provider.getWeb3j();
        try {
            Function nameFn = new Function(
                    "name",
                    Collections.emptyList(),
                    Arrays.asList(new TypeReference<Utf8String>() {})
            );

            EthCall call = web3.ethCall(
                    Transaction.createEthCallTransaction(null, tokenAddress, FunctionEncoder.encode(nameFn)),
                    DefaultBlockParameterName.LATEST
            ).send();

            List<Type> decoded = FunctionReturnDecoder.decode(call.getValue(), nameFn.getOutputParameters());
            if (!decoded.isEmpty()) {
                return decoded.get(0).getValue().toString();
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error fetching name for " + tokenAddress + ": " + e.getMessage());
        }
        return "UNKNOWN";
    }
}
