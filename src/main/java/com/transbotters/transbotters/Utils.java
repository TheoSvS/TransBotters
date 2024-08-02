package com.transbotters.transbotters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.http.HttpStatus;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class Utils {

    private static final String ERC20_TRANSFER_FUNCTION_SIGNATURE = "a9059cbb"; // transfer(address,uint256)
    private static final String ERC20_BALANCEOF_FUNCTION_SIGNATURE = "70a08231"; // balanceOf(address)

    private static final String EVENT_ERC20_TRANSFER_FUNCTION_SIGNATURE = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";


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

    public static boolean isERC20TokenCreation(String inputData) {
        // Simplified check for ERC-20 method signatures
        return inputData.contains(ERC20_TRANSFER_FUNCTION_SIGNATURE) && inputData.contains(ERC20_BALANCEOF_FUNCTION_SIGNATURE);
    }


    public static String getCreatedTokenAddress(TransactionDetailsDTO transactionDetailsDTO){
        return transactionDetailsDTO.getTransactionReceipt().getContractAddress();
    }


    public static String getSecurityResponse(String tokenAddress) {
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
        System.out.println(prettyJson);

        return json.get("honeypotResult").getAsJsonObject().get("isHoneypot").getAsString();
    }


    // Method to parse JSON and print it
    private static JsonObject parseJson(String responseBody) {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        System.out.println(json.toString()); // Print JSON
        return json;
    }



/*    public static String getContractAddressOfCreationTx(List<Log> logs){
        for (Log log : logs) {
            // Check if this log matches the Transfer event signature
            if (log.getTopics().get(0).equals(EVENT_ERC20_TRANSFER_FUNCTION_SIGNATURE)) {
                String fromAddress = "0x" + log.getTopics().get(1).substring(26);
                String toAddress = "0x" + log.getTopics().get(2).substring(26);

                System.out.println("Transfer detected:");
                System.out.println("From: " + fromAddress);
                System.out.println("To: " + toAddress);
                return toAddress;
            }
        }
        return null;
    }*/
}
