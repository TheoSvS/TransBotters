package com.transbotters.transbotters;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

@Component
@Slf4j
public class ScannerBot {
    private  DateTimeFormatter formatter;

    private final Web3j web3j;
    private final Disposable disposableSubSuccessTxs;

    Map<String, TransactionDetailsDTO> transactionDetailsMap = Collections.synchronizedMap(new HashMap<>());


    public ScannerBot(RPCProvider rpcProvider) {
       initDateTimeFormatter();
        web3j = Web3j.build(new HttpService(rpcProvider.getALCHEMY_URL()));
        this.disposableSubSuccessTxs = startDisposableSuccessTxSubscriber();
    }


    private @NotNull Disposable startDisposableSuccessTxSubscriber() {
        final Disposable disposableSubscription;
        disposableSubscription = startDisposableTxSubscriber();
        return disposableSubscription;
    }

    private @NotNull Disposable startDisposableTxSubscriber() {
        final Disposable disposableSubscription;
        Flowable<Transaction> transactionFlowable = web3j.transactionFlowable();
        // Subscribe to transactions with retry logic and error handling
        disposableSubscription = getTransactionDetatils(transactionFlowable);
        return disposableSubscription;
    }

    private @NotNull Disposable getTransactionDetatils(Flowable<Transaction> transactionFlowable) {
        return transactionFlowable
                .retryWhen(errors ->
                        errors
                                .zipWith(Flowable.range(1, 50), (error, retryCount) -> retryCount) //retry 50 times if there is an error (e.g. network instability/delay
                                .flatMap(retryCount -> Flowable.timer(2, TimeUnit.SECONDS)) //retry every 2seconds
                )
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .parallel(4)
                .runOn(Schedulers.newThread())
                .flatMap(transaction ->
                        Flowable.fromFuture(web3j.ethGetTransactionReceipt(transaction.getHash()).sendAsync())
                                .map(EthGetTransactionReceipt::getTransactionReceipt)
                                .map(transactionReceipt -> new TransactionDetailsDTO(transaction, transactionReceipt.get()))
                                .filter(tranDetailsOptional -> filterTransactionByTransactionType( tranDetailsOptional))
                                .timeout(5, TimeUnit.SECONDS) // Timeout for each transactionReceipt request
                                .onErrorResumeNext(error -> {
                                    if (error instanceof TimeoutException) {
                                        System.err.println("Timeout occurred while processing transaction: " + transaction.getHash());
                                        return Flowable.empty(); // Skip this transaction and continue with the next one
                                    } else {
                                        return Flowable.error(error); // Propagate other errors
                                    }
                                })
                )
                .sequential()
                .subscribe(transactionDetailsDTO -> {
                    this.handleTransactionByType(transactionDetailsDTO);
                }, throwable -> {
                    System.err.println("Error in Flowable subscription: " + throwable.getMessage());
                    throwable.printStackTrace();
                });
    }

    private void handleTransactionByType(TransactionDetailsDTO transactionDetailsDTO) {
        if(Utils.isERC20TokenCreation(transactionDetailsDTO.getTransaction().getInput())){
            handleTokenCreationTransaction(transactionDetailsDTO);
        } else if(Utils.isERC20TokenCreation(transactionDetailsDTO.getTransaction().getInput())){
            handleAddLiquidityTransaction(transactionDetailsDTO);
        } else {
            handleTokenBoughtTransaction(transactionDetailsDTO);
        }
    }

    private void handleTokenBoughtTransaction(TransactionDetailsDTO transactionDetailsDTO) {
        String tokenAddress = transactionDetailsDTO.getTransactionReceipt().getContractAddress();
        log.info("Found transaction for transfer to token with transaction  https://etherscan.io/tx/"+ transactionDetailsDTO.getTransaction().getHash());
        log.info("Token"+ " bought for: " +  Convert.fromWei(transactionDetailsDTO.getTransaction().getValue().toString(), Convert.Unit.ETHER) + " ETH"); // Convert to ETH if needed
    }

    private boolean filterTransactionByTransactionType(TransactionDetailsDTO transactionDetails) {
        return  Utils.isERC20TokenCreation(transactionDetails.getTransaction().getInput()) || //creation
                Utils.isERC20AddLiquidity(transactionDetails.getTransaction().getInput()) || //add liquidity
                Utils.isERC20TransferMethod(transactionDetails) && isTokenBought(transactionDetails); //token bought
    }

    private void handleTokenCreationTransaction(TransactionDetailsDTO transactionDetailsDTO) {
        String createdTokenAddress = Utils.getCreatedTokenAddress(transactionDetailsDTO);
        var honeyPot = Utils.getSecurityResponse(createdTokenAddress);
        if(honeyPot){
            log.info("Found honeypot token=>" + createdTokenAddress);
        }
//        if (Utils.getSecurityResponse(createdTokenAddress)) {
//            log.info("Found honeypot token=>" + createdTokenAddress);
//        } else {
            if (Objects.nonNull(createdTokenAddress)) {
                transactionDetailsMap.put(createdTokenAddress, transactionDetailsDTO);
                log.info("Token creation discovered with transaction https://etherscan.io/tx/" + transactionDetailsDTO.getTransaction().getHash());
                //TODO Need to distinguish if token address or contact address
                log.info("https://etherscan.io/token/" + createdTokenAddress);
            }
        //}
    }

    private boolean isTokenBought(TransactionDetailsDTO transactionDetails) {
        return transactionDetails.getTransactionReceipt().getLogs().stream().anyMatch(log ->
                log.getTopics().size()>=3 && transactionDetailsMap.containsKey(log.getTopics().get(2)));
    }

    private void handleAddLiquidityTransaction(TransactionDetailsDTO transactionDetailsDTO) {
        String createdTokenAddress = Utils.getCreatedTokenAddress(transactionDetailsDTO);
        if(transactionDetailsMap.containsKey(createdTokenAddress)){
            log.info("Liquidity added https://etherscan.io/tx/" + transactionDetailsDTO.getTransaction().getHash());
            log.info("https://etherscan.io/token/" + createdTokenAddress);
        }
    }

/*      if str(decoded[
            0]) == '<Function addLiquidityETH(address,uint256,uint256,uint256,address,uint256)>' or str(
            decoded[
            0]) == '<Function addLiquidity(address,address,uint256,uint256,uint256,uint256,address,uint256)>' or str(
           */

    private void initDateTimeFormatter(){
        // Define a formatter for the date and time
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    }

    @PreDestroy
    public void cleanup() {
        this.disposableSubSuccessTxs.dispose();
        this.web3j.shutdown();
    }
}
