package com.transbotters.transbotters.contractcalls;

import com.transbotters.transbotters.web3.Web3Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

@Component
@Slf4j
public class TokenSwap {
    private final Web3Provider web3Provider;
    private final Web3j web3j;
    private final String uniswapV2Router;
    private final String transBotAddr;
    private final Credentials credentials;
    private final TransactionManager transactionManager;
    private final StaticGasProvider gasProvider;

    public TokenSwap(Web3Provider web3Provider) {
        this.web3Provider = web3Provider;
        this.web3j = web3Provider.getWeb3j();
        this.uniswapV2Router = web3Provider.getEthUniswapV2Router();
        this.transBotAddr = web3Provider.getBotWalletAddr();

        //TODO: need secure storing and parsing of private key
        this.credentials = Credentials.create(web3Provider.getPrivateKey());

        // Transaction Manager
        this.transactionManager = new RawTransactionManager(web3j, credentials);
        this.gasProvider = new StaticGasProvider(DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT);
    }

    public void swapExactETHForTokens(String tokenToBuy) throws IOException {

        Function swapExactETHFunc = getSwapExactETHFunction(tokenToBuy);

        // Set the amount of ETH to swap
        //TODO: testing with 0.0005ETH ~ 1.30 dai
        BigInteger amountETHToSwap = Convert.toWei("0.005", Convert.Unit.ETHER).toBigInteger();

        //Simulating Transaction (it could fail if we don't have enough ETH in the wallet etc)
        if (!isSuccessfulTxSimulation(swapExactETHFunc)) {
            return;
        }

        // Sending transaction
        EthSendTransaction ethSendTransaction = transactionManager.sendTransaction(
                gasProvider.getGasPrice(),
                gasProvider.getGasLimit(),
                uniswapV2Router,
                FunctionEncoder.encode(swapExactETHFunc),
                amountETHToSwap
        );

        ethSendTransaction.getTransactionHash();
        log.info("Token swap transaction hash: " + ethSendTransaction.getTransactionHash());
    }

    Function getSwapExactETHFunction(String tokenBuyAddr) {
        String wethAddr = web3Provider.getWethAddress();

        //TODO: declares the minimum we want to receive(avoid slippage!!!)
        // Specify the minimum amount of tokens you want to receive (e.g., 1 DAI)
        BigInteger amountOutMin = BigInteger.valueOf(1); // Adjust based on the decimals of the token

        // Deadline for the transaction
        BigInteger deadlineSeconds = BigInteger.valueOf(System.currentTimeMillis() / 1000 + 60 * 2);

        DynamicArray<Address> swapRoutingPath = new DynamicArray<>(Address.class, new Address(wethAddr), new Address(tokenBuyAddr));

        // Function call for swapExactETHForTokens
        Function swapExactETHFunc = new Function(
                "swapExactETHForTokens",
                Arrays.asList(
                        new Uint256(amountOutMin),    //TODO: declares the minimum we want to receive(avoid slippage!!!)
                        swapRoutingPath,  //addresses of swapping path e.g. WETH -> LULZ (must be >=2 )
                        new Address(credentials.getAddress()),  //recipient
                        new Uint256(deadlineSeconds)
                ),
                Collections.emptyList());

        return swapExactETHFunc;
    }

    void approveTokenSpending(String tokenToBuy, BigInteger tokenAmountToApprove) throws IOException {

        // Amount to approve (in wei, as BigInteger)
        //BigInteger tokenAmountToApprove = BigInteger.valueOf(100_000_000_000_000_000L); // 100 DAI in wei

        // Create the approval function
        Function approveFunction = new Function(
                "approve",
                Arrays.asList(new Address(uniswapV2Router), new Uint256(tokenAmountToApprove)),
                Collections.emptyList()
        );

        // Encode the function
        String encodedFunction = FunctionEncoder.encode(approveFunction);

        // Send the approval transaction
        EthSendTransaction approveTransaction = transactionManager.sendTransaction(
                gasProvider.getGasPrice(),
                gasProvider.getGasLimit(),
                tokenToBuy,
                encodedFunction,
                BigInteger.ZERO // No ETH value is sent in this transaction
        );

        // Wait for the transaction to be mined (optional)
        String approveTransactionHash = approveTransaction.getTransactionHash();
    }

    /**
     * eth_call method of web3 simulates the transaction before it is actually sent, to ensure that it won't be reverted (e.g. if we don't have enough ETH )
     *
     * @return if the call simulation responds that the transaction would succeed or fail
     */
    boolean isSuccessfulTxSimulation(Function swapExactETHFunc) throws IOException {

        // Create the transaction object
        Transaction transaction = Transaction.createEthCallTransaction(
                transBotAddr, // from address
                uniswapV2Router, // to address (contract address)
                FunctionEncoder.encode(swapExactETHFunc) // encoded function call
        );


        // Execute the eth_call
        EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

        // Check for errors or reverts
        String result = response.getValue();
        if (result != null && !result.isEmpty()) {
            log.info("Simulation successful, no revert detected.");
            log.info("Result: " + result);
            return true;
        } else {
            log.info("Transaction may be rejected or reverted.");
            if (response.getError() != null) {
                log.info("Error: " + response.getError().getMessage());
            }
            return false;
        }
    }
}
