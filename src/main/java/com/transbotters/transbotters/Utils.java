package com.transbotters.transbotters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Utils {

    /**
     * Add liquidity
     */
    static final String ERC20_TRANSFER_FUNCTION_SIGNATURE = "a9059cbb"; // transfer(address,uint256)  Transfers a specific amount of tokens from the sender to the recipient.
    static final String ERC20_BALANCEOF_FUNCTION_SIGNATURE = "70a08231"; // balanceOf(address)
    static final String ERC20_ADD_LIQUIDITY_FUNCTION_SIGNATURE ="e8e33700"; //String methodSignature = "addLiquidity(address,address,uint256,uint256,uint256,uint256,address,uint256)";
    static final String ERC20_ADD_LIQUIDITY_ETH_FUNCTION_SIGNATURE ="f305d719"; //addLiquidityETH(address token,uint256 amountTokenDesired,uint256 amountTokenMin,uint256 amountETHMin,address to,uint256 deadline )
    //Balancer V2:
    static final String JOIN_POOL_BALANCER_V2 ="b04e41c3"; //joinPool(bytes32 poolId, address sender, address recipient, JoinPoolRequest memory request) Adds liquidity to a Balancer pool using the new Vault system.
    //Sushi swap
    static final String ADD_LIQUIDITY_SUSHI_SWAP ="f305d719"; // : addLiquidityETH(address token, uint256 amountTokenDesired, uint256 amountTokenMin, uint256 amountETHMin, address to, uint256 deadline) Adds liquidity to a token-ETH pair on SushiSwap.

    /**
     * Transfer
     */
    static final String ERC20_SWAP_ETH_FOR_EXACT_TOKENS = "fb3bd41"; // function swapETHForExactTokens(uint256 amountOut, address[] calldata path, address to, uint256 deadline) external payable;
    static final String TRANSFER_FROM = "23b872dd"; // transferFrom(address sender, address recipient, uint256 amount) Transfers a specific amount of tokens on behalf of a user, often used when the contract is approved to spend the tokens.
    static final String SWAP_ETH_FOR_TOKENS =  "38ed1739"; //swapExactTokensForTokens(uint256 amountIn, uint256 amountOutMin, address[] path, address to, uint256 deadline) Swaps an exact amount of input tokens for as many output tokens as possible.
    static final String SWAP_EXACT_ETH_FOR_TOKENS = "8803dbee"; //swapExactETHForTokens(uint256 amountOutMin, address[] path, address to, uint256 deadline) //Swaps an exact amount of ETH for as many output tokens as possible
    static final String SWAP_ETH_FOR_EXACT_TOKENS = "fb3bdb41"; //swapETHForExactTokens(uint256 amountOut, address[] path, address to, uint256 deadline)  Swaps ETH for a specific amount of tokens
    static final String SWAP_EXACT_TOKENS_FOR_ETH = "7ff36ab5"; //swapExactTokensForETH(uint256 amountIn, uint256 amountOutMin, address[] path, address to, uint256 deadline) Swaps an exact amount of tokens for ETH.
    static final String SWAP_TOKENS_FOR_EXACT_TOKENS = "5c11d795"; //swapTokensForExactTokens(uint256 amountOut, uint256 amountInMax, address[] path, address to, uint256 deadline) Swaps as few input tokens as possible for an exact amount of output tokens
    static final String SWAP_EXACT_TOKENS_FOR_TOKENS_FEE = "18cbafe5"; // swapExactTokensForTokensSupportingFeeOnTransferTokens(uint256 amountIn, uint256 amountOutMin, address[] path, address to, uint256 deadline)  //Swaps an exact amount of tokens for another token, supporting tokens with transfer fees.
    static final String SWAP_EXACT_TOKEN_FOR_ETH_FEE = "4a25d94a"; // swapExactTokensForETHSupportingFeeOnTransferTokens(uint256 amountIn, uint256 amountOutMin, address[] path, address to, uint256 deadline)  //Swaps an exact amount of tokens for ETH, supporting tokens with transfer fees
    static final String SWAP_TOKENS_FOR_EXACT_ETH = "791ac947"; // swapTokensForExactETH(uint256 amountOut, uint256 amountInMax, address[] path, address to, uint256 deadline) Swaps as few tokens as possible for an exact amount of ETH.
    static final String FLASH_SWAP ="d0e30db0";  //flashSwap(address tokenBorrow, uint256 amountBorrow, bytes calldata data) Flash Swaps: Borrowing assets in a swap and repaying within the same transaction.
    static final String SWAP ="d78ad95f"; // Swap(address indexed sender, uint256 amount0In, uint256 amount1In, uint256 amount0Out, uint256 amount1Out, address indexed to)  //Event emitted during a token swap.
    //1inch Aggregator:
    static final String INCH_AGGREGATOR_SWAP = "7c025200";// swap(address executor, SwapDescription calldata desc, bytes calldata permit, bytes calldata data) Executes a swap through multiple DEXs for optimal routing.
    //1inch
    static final String UNO_SWAP = "b040d545"; //unoswap(address srcToken, uint256 amount, uint256 minReturn, bytes32[] calldata pools) A direct swap function using the Uniswap V3 pool.
    static final String UNO_SWAP_2 = "d9627aa6"; //unoswap(address srcToken, uint256 amount, uint256 minReturn, bytes32[] calldata pools, uint256[] calldata swapData) A more complex swap function allowing interactions with Uniswap V3 pools.
    //Balancer V2
    static final String SWAP_BALANCE_V2 ="30496130"; //: swap(SingleSwap memory singleSwap, FundManagement memory funds, uint256 limit, uint256 deadline)  Performs a token swap through the Balancer Vault.
    //SushiSwap
    static final String SUSHI_FLASH_SWAP = "d0e30db0"; // flashSwap(address tokenBorrow, uint256 amountBorrow, bytes calldata data) Similar to Uniswap V2, this enables flash swaps in SushiSwap.
    //Uniswap
    static final String BUY_UNISWAP ="c27bba7f"; // buy(uint256 amountOut, address[] path, address to, uint256 deadline)  function buy(uint256 amountOut, address[] calldata path, address to, uint256 deadline) external;

    /**
     * Remove liquidity
     */
    static final String REMOVE_LIQUIDITY = "afc2ab49"; //removeLiquidity(address tokenA, address tokenB, uint256 liquidity, uint256 amountAMin, uint256 amountBMin, address to, uint256 deadline)  Removes liquidity from a token pair
    static final String REMOVE_LIQUIDITY_ETH = "02751cec"; //removeLiquidityETH(address token, uint256 liquidity, uint256 amountTokenMin, uint256 amountETHMin, address to, uint256 deadline) Removes liquidity from a token-ETH pair.
    static final String REMOVE_LIQUIDITY_ETH_WITH_PERMIT ="ded9382a"; //removeLiquidityETHWithPermit(address token, uint256 liquidity, uint256 amountTokenMin, uint256 amountETHMin, address to, uint256 deadline, bool approveMax, uint8 v, bytes32 r, bytes32 s) Removes liquidity from a token-ETH pair with signature-based permission (permit).
    static final String REMOVE_LIQUIDITY_WITH_PERMIT = "2195995c"; // removeLiquidityWithPermit(address tokenA, address tokenB, uint256 liquidity, uint256 amountAMin, uint256 amountBMin, address to, uint256 deadline, bool approveMax, uint8 v, bytes32 r, bytes32 s) Removes liquidity from a token pair with signature-based permission (permit).
    static final String BALANCER_V2_EXIT_POOL ="0287a6e0" ; //balancer v2 exit pool  exitPool(bytes32 poolId, address sender, address recipient, ExitPoolRequest memory request) Removes liquidity from a Balancer pool.


    /**
     * Other not mapped yet
     */
    //approval
    //095ea7b3: approve(address spender, uint256 amount)  Approves a spender to use a specified amount of tokens on behalf of the caller.
    //events  0xddf252ad: Transfer(address indexed from, address indexed to, uint256 value) Event emitted when a transfer of tokens occur
    //8c5be1e5: Approval(address indexed owner, address indexed spender, uint256 value) //Event emitted when an approval is made for a spender to use tokens on behalf of an owner.
    //mint  682d252c: mint(address to, uint256 amount) Mints a specified amount of tokens to the given address.
    //burn 0xa0712d68: burn(uint256 amount) Burns a specified amount of tokens.
    ///0x1c411e9a: Sync(uint112 reserve0, uint112 reserve1)  Event emitted when a pool's reserves are updated, which occurs during swaps or liquidity changes.
    //PancakeSwap (BSC): For BNB ony
    //4e71d92d: swapExactTokensForTokensSupportingFeeOnTransferTokens(uint256 amountIn, uint256 amountOutMin, address[] calldata path, address to, uint256 deadline) Swaps tokens on PancakeSwap while supporting fee-on-transfer tokens.
    //c6a84c3c: swapExactBNBForTokens(uint256 amountOutMin, address[] calldata path, address to, uint256 deadline) Swaps BNB for tokens on PancakeSwap.

    static final List<String> ADD_LIQUIDITY_FUNCTIONS = List.of(ERC20_TRANSFER_FUNCTION_SIGNATURE,
            ERC20_BALANCEOF_FUNCTION_SIGNATURE, ERC20_ADD_LIQUIDITY_FUNCTION_SIGNATURE,
            ERC20_ADD_LIQUIDITY_ETH_FUNCTION_SIGNATURE, JOIN_POOL_BALANCER_V2, ADD_LIQUIDITY_SUSHI_SWAP);

    static final List<String> TRANSFER_FUNCTIONS = List.of(ERC20_TRANSFER_FUNCTION_SIGNATURE, ERC20_SWAP_ETH_FOR_EXACT_TOKENS, TRANSFER_FROM, SWAP_ETH_FOR_TOKENS,
            SWAP_EXACT_ETH_FOR_TOKENS, SWAP_ETH_FOR_EXACT_TOKENS, SWAP_EXACT_TOKENS_FOR_ETH, SWAP_TOKENS_FOR_EXACT_TOKENS,
            SWAP_EXACT_TOKENS_FOR_TOKENS_FEE, SWAP_EXACT_TOKEN_FOR_ETH_FEE, SWAP_TOKENS_FOR_EXACT_ETH, FLASH_SWAP, SWAP,
            INCH_AGGREGATOR_SWAP, UNO_SWAP, UNO_SWAP_2, SWAP_BALANCE_V2, SUSHI_FLASH_SWAP, BUY_UNISWAP
    );

    static final List<String> REMOVE_LIQUIDITY_FUNCTIONS = List.of(REMOVE_LIQUIDITY, REMOVE_LIQUIDITY_ETH,
            REMOVE_LIQUIDITY_ETH_WITH_PERMIT, REMOVE_LIQUIDITY_WITH_PERMIT, BALANCER_V2_EXIT_POOL);

    /**
     * Takes an Ethereum "decimal" (BigInteger representation multiplied by 10^x where x are the decimal places) and converts to BigDecimal
     *
     * @param ethDecRepresentation The Ethereum decimal represented as a BigInteger
     * @param ethDecimals          The decimal places represented in the BigInteger
     * @param outputDecimals       The decimal places of the output BigDecimal
     * @return The output BigDecimal
     */
    public static BigDecimal ethDecToBigDecimal(BigInteger ethDecRepresentation, int ethDecimals, int outputDecimals) {
        return new BigDecimal(ethDecRepresentation).divide(BigDecimal.valueOf(Math.pow(10, ethDecimals)), outputDecimals, RoundingMode.HALF_UP);
    }

    /** Checks if an input contains any of the substrings
     * @param input The string to check
     * @param toMatch The strings to match
     * @return True if the input contains any of the substrings
     */
    public static boolean containsAny(String input, String... toMatch){
        for(String s : toMatch){
            if(input.contains(s)){
                return true;
            }
        }
        return false;
    }

    public static boolean findEmptyProperties(String... properties){
        Arrays.stream(properties).anyMatch(property-> property==null || property.isBlank());
        return Arrays.stream(properties).anyMatch(property-> property==null || property.isBlank());
    }

    public static boolean isERC20TokenCreation(String inputData) {
        // Simplified check for ERC-20 method signatures
        return inputData.contains(ERC20_TRANSFER_FUNCTION_SIGNATURE) && inputData.contains(ERC20_BALANCEOF_FUNCTION_SIGNATURE);
    }

    public static boolean isERC20AddLiquidity(String inputData) {
        // Simplified check for ERC-20 method signatures
        return ADD_LIQUIDITY_FUNCTIONS.stream().anyMatch(inputData::contains);
    }

    public static boolean isERC20TransferMethod(String inputData) {
        // Simplified check for ERC-20 method signatures
        return TRANSFER_FUNCTIONS.stream().anyMatch(inputData::contains);
    }

    public static boolean isERC20RemoveLiquidity(String inputData){
        return REMOVE_LIQUIDITY_FUNCTIONS.stream().anyMatch(inputData::contains);
    }


    public static String getCreatedTokenAddress(TransactionDetailsDTO transactionDetailsDTO){
        return transactionDetailsDTO.getTransactionReceipt().getContractAddress();
    }

    public static boolean getSecurityResponse(String tokenAddress) {
        // Create an HttpClient instance
        HttpClient client = HttpClient.newHttpClient();

        // Build the HttpRequest
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.honeypot.is/v2/IsHoneypot?address="+tokenAddress))
                .header("Accept", "application/json")
                .build();

        // Send the request and get the response
        JsonObject json = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(Utils::parseJson)
                .join();

        // Check the response status code
/*            if (json.get("isHoneypot").getAsBoolean()) {
                System.out.println("HONIPOTA");
            }*/
        // Create a Gson instance with pretty printing enabled
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Convert JsonObject to a pretty-printed JSON string
        String prettyJson = gson.toJson(json);

        // Print the pretty-printed JSON string
        //System.out.println(prettyJson);
        if(Objects.nonNull(json.get("honeypotResult"))){
            return  !"true".equals(json.get("honeypotResult").getAsJsonObject().get("isHoneypot"));
        }
        //if is not honeypot or cannot know for sure for now add it on list
        return false;
    }


    // Method to parse JSON and print it
    private static JsonObject parseJson(String responseBody) {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        //System.out.println(json.toString()); // Print JSON
        return json;
    }
}
