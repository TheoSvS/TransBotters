package com.transbotters.transbotters;

import com.transbotters.transbotters.web3.Web3Provider;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.utils.Convert;
//
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

@Component
@Slf4j
public class ScannerBot{

    private Web3Provider web3Provider;

    private Disposable disposableSubSuccessTxs;

    Map<String, TransactionDetailsDTO> transactionDetailsMap = Collections.synchronizedMap(new HashMap<>());

    public ScannerBot(Web3Provider web3Provider) {
        this.web3Provider = web3Provider;
    }

    @PostConstruct
    public void  startScannerBot() {
        this.disposableSubSuccessTxs = startDisposableSuccessTxSubscriber();
    }


    private @NotNull Disposable startDisposableSuccessTxSubscriber() {
        final Disposable disposableSubscription;
        disposableSubscription = startDisposableTxSubscriber();
        return disposableSubscription;
    }

    private @NotNull Disposable startDisposableTxSubscriber() {
        final Disposable disposableSubscription;
        Flowable<Transaction> transactionFlowable = web3Provider.getWeb3j().transactionFlowable();
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
                        Flowable.fromFuture(web3Provider.getWeb3j().ethGetTransactionReceipt(transaction.getHash()).sendAsync())
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
                    log.error(throwable.getMessage());
                });
    }

    private void handleTransactionByType(TransactionDetailsDTO transactionDetailsDTO) {
        if(Utils.isERC20TokenCreation(transactionDetailsDTO.getTransaction().getInput())){
            handleTokenCreationTransaction(transactionDetailsDTO);
        } else if(Utils.isERC20TokenCreation(transactionDetailsDTO.getTransaction().getInput())){
            handleAddLiquidityTransaction(transactionDetailsDTO);
        } else if(isTokenBought(transactionDetailsDTO)){
            handleTokenBoughtTransaction(transactionDetailsDTO);
        } else if(isLiquidityRemoved(transactionDetailsDTO)){
            handleRemoveLiquidityTransaction(transactionDetailsDTO);
        }
    }

    private void handleRemoveLiquidityTransaction(TransactionDetailsDTO transactionDetailsDTO) {
        String tokenAddress = transactionDetailsDTO.getTransactionReceipt().getContractAddress();
        if(Objects.nonNull(tokenAddress) && !transactionDetailsMap.isEmpty() && transactionDetailsMap.containsKey(tokenAddress)){
            log.info("Found transaction for remove liquidity from token transaction  https://etherscan.io/tx/"+ transactionDetailsDTO.getTransaction().getHash());
            //TODO should remove token if liquidity removed
            //transactionDetailsMap.remove(tokenAddress);
        }
    }

    private void handleTokenBoughtTransaction(TransactionDetailsDTO transactionDetailsDTO) {
        //TODO contract address could be null so token address should be found
        //String tokenAddress = transactionDetailsDTO.getTransactionReceipt().getContractAddress();
        log.info("Found transaction for transfer to token with transaction  https://etherscan.io/tx/"+ transactionDetailsDTO.getTransaction().getHash());
        log.info("Token"+ " bought for: " +  Convert.fromWei(transactionDetailsDTO.getTransaction().getValue().toString(), Convert.Unit.ETHER) + " ETH"); // Convert to ETH if needed
    }

    private boolean filterTransactionByTransactionType(TransactionDetailsDTO transactionDetails) {
        return  Utils.isERC20TokenCreation(transactionDetails.getTransaction().getInput()) || //creation
                Utils.isERC20AddLiquidity(transactionDetails.getTransaction().getInput()) || //add liquidity
                isTokenBought(transactionDetails) || //token bought
                isLiquidityRemoved(transactionDetails); // remove liquidity
    }

    private boolean isLiquidityRemoved(TransactionDetailsDTO transactionDetailsDTO){
        String createdTokenAddress = Utils.getCreatedTokenAddress(transactionDetailsDTO);
        return !transactionDetailsMap.isEmpty() &&
                Utils.isERC20RemoveLiquidity(transactionDetailsDTO.getTransaction().getInput())
                && transactionDetailsMap.containsKey(createdTokenAddress);
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
            log.info("Token creation discovered with transaction " + web3Provider.getChainExplorerURL() + "/tx/" + transactionDetailsDTO.getTransaction().getHash());
            //TODO Need to distinguish if token address or contact address
            log.info(web3Provider.getChainExplorerURL() + "/token/" + createdTokenAddress);
        }
        //}
    }

    private boolean isTokenBought(TransactionDetailsDTO transactionDetails) {
        boolean isNotEmpty = !transactionDetailsMap.isEmpty();
        boolean isTransfer = Utils.isERC20TransferMethod(transactionDetails.getTransaction().getInput());
        boolean isToAddress = transactionDetailsMap.containsKey(transactionDetails.getTransaction().getTo()) ||
                transactionDetails.getTransactionReceipt().getLogs().stream().anyMatch(log ->
                        log.getTopics().size()>=3 && transactionDetailsMap.containsKey(log.getTopics().get(2)));
        boolean isValueAboveZero = transactionDetails.getTransaction().getValue().compareTo(BigInteger.ZERO)>0;
        return isNotEmpty && isTransfer &&  isToAddress && isValueAboveZero;
    }

    private void handleAddLiquidityTransaction(TransactionDetailsDTO transactionDetailsDTO) {
        String createdTokenAddress = Utils.getCreatedTokenAddress(transactionDetailsDTO);
        if(transactionDetailsMap.containsKey(createdTokenAddress)){
            log.info("Liquidity added https://etherscan.io/tx/" + transactionDetailsDTO.getTransaction().getHash());
            log.info("https://etherscan.io/token/" + createdTokenAddress);
        }
    }

    @PreDestroy
    public void cleanup() {
        this.disposableSubSuccessTxs.dispose();
    }
}
