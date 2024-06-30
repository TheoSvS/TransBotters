package com.transbotters.transbotters;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class Utils {

    private static final String ERC20_TRANSFER_FUNCTION_SIGNATURE = "a9059cbb"; // transfer(address,uint256)
    private static final String ERC20_BALANCEOF_FUNCTION_SIGNATURE = "70a08231"; // balanceOf(address)

    private static final String ERC20_TRANSFER_EVENT_SIGNATURE = "ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";



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
}
