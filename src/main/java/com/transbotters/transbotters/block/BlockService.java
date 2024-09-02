package com.transbotters.transbotters.block;

import com.transbotters.transbotters.ETransactionType;
import com.transbotters.transbotters.Utils;
import com.transbotters.transbotters.web3.Web3Provider;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionObject;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * Service that provides function calls on specific blocks
 */
@Component
@Slf4j
public class BlockService{

    private Web3Provider web3Provider;

    Map<String, TransactionObject> transactionDetailsMap = new HashMap<>();

    public BlockService(Web3Provider web3Provider) {
        this.web3Provider = web3Provider;
    }

    private List<TransactionObject> getTransactionsByBlockNumber(BigInteger blockNumber){
        try {
            // Fetch the block with transaction details
            EthBlock block = web3Provider.getWeb3j().ethGetBlockByNumber(DefaultBlockParameter.valueOf(blockNumber), true).send();
            // Print out block details
            log.info("Block Number: " + block.getBlock().getNumber());
            log.info("Block Hash: " + block.getBlock().getHash());
            log.info("Block Transactions: " + block.getBlock().getTransactions().size());
            // Iterate over transactions in the block
            return block.getBlock().getTransactions().stream().map(TransactionObject.class::cast).toList();
        } catch (IOException e){
            log.error("Could not read block:"+blockNumber.toString()+"\n"+e.getMessage());
            return Collections.emptyList();
        }
    }

    private void printTransactionDetails(TransactionObject tx){
        TransactionObject transaction = (TransactionObject) tx.get();
        log.info("Transaction Hash: " + transaction.getHash());
        log.info("From: " + transaction.getFrom());
        log.info("To: " + transaction.getTo());
        log.info("Value: " + transaction.getValue());
        log.info("Gas Price: " + transaction.getGasPrice());
        log.info("Gas Used: " + transaction.getGas());
        log.info("Input Data: " + transaction.getInput());
        log.info("====================================");
    }

    public void readTransactionByBlockRange(@NotNull BigInteger blockStart,@NotNull BigInteger blockEnd, ETransactionType transactionType) throws IOException {
        for(BigInteger blockElement = blockStart; blockElement.compareTo(blockEnd)<=0;blockElement = blockElement.add( BigInteger.ONE)){
            readBlockTransactionByType(blockElement, transactionType);
        }
    }

    public void readBlockTransactionByType(BigInteger blockNumber, ETransactionType type) {
        switch (type) {
            case TOKEN_CREATION -> handleTokenCreationTransaction(blockNumber);
            case TOKEN_BOUGHT -> handleTokenBoughtTransaction(blockNumber);
            case ADD_LIQUIDITY -> handleTokenAddLiquidity(blockNumber);
            case REMOVE_LIQUIDITY -> handleRemoveLiquidityTransaction(blockNumber);
        }
    }

    private void handleTokenCreationTransaction(BigInteger blockNumber) {
        getTransactionsByBlockNumber(blockNumber).stream().filter(transactionObject ->
                Utils.isERC20TokenCreation(transactionObject.getInput())).forEach(transactionObject -> {
            transactionDetailsMap.put(transactionObject.getCreates(), transactionObject);
            log.info("Token creation discovered with transaction https://etherscan.io/tx/" + transactionObject.get().getHash());
            log.info("https://etherscan.io/token/" + transactionObject.getCreates());
        });
    }

    private void handleTokenBoughtTransaction(BigInteger blockNumber) {
        getTransactionsByBlockNumber(blockNumber).stream().filter(transactionObject ->
                Utils.isERC20TransferMethod(transactionObject.getInput()) &&
                        transactionObject.getValue().compareTo(BigInteger.ZERO)>0
        ).forEach(transactionObject -> {
            log.info("Found transaction for transfer to token with transaction  https://etherscan.io/tx/"+ transactionObject.get().getHash());
            log.info("Token"+ transactionObject.getTo() +" bought for: " +  Convert.fromWei(transactionObject.getValue().toString(), Convert.Unit.ETHER) + " ETH");
        });
    }

    private void handleTokenAddLiquidity(BigInteger blockNumber){
        getTransactionsByBlockNumber(blockNumber).stream().filter(transactionObject ->
                Utils.isERC20AddLiquidity(transactionObject.getInput())).forEach(transactionObject -> {
            log.info("Liquidity added https://etherscan.io/tx/" + transactionObject.get().getHash());
        });
    }

    private void handleRemoveLiquidityTransaction(BigInteger blockNumber) {
        getTransactionsByBlockNumber(blockNumber).stream().filter(transactionObject ->
                Utils.isERC20RemoveLiquidity(transactionObject.getInput())).forEach(transactionObject -> {
            log.info("Found transaction for remove liquidity from token transaction  https://etherscan.io/tx/"+ transactionObject.get().getHash());
        });
    }

}
