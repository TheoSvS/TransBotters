package com.transbotters.transbotters;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.http.HttpService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Component
public class ScannerBot {
    private  DateTimeFormatter formatter;

    private final Web3j web3j;
    private final Disposable disposableSubSuccessTxs;

    List<TransactionDetailsDTO> transactionDetailsDTOList = Collections.synchronizedList(new ArrayList<>());


    public ScannerBot(RPCProvider rpcProvider) {
       initDateTimeFormatter();

        web3j = Web3j.build(new HttpService(rpcProvider.getALCHEMY_URL()));
        this.disposableSubSuccessTxs = startDisposableSuccessTxSubscriber();

    }


    private @NotNull Disposable startDisposableSuccessTxSubscriber() {
        final Disposable disposableSubscription;
        disposableSubscription = startDisposableTxSubscriber(ETransactionType.TOKEN_CREATION);
        return disposableSubscription;
    }



    private @NotNull Disposable startDisposableTxSubscriber(ETransactionType desiredTransactionType) {
        final Disposable disposableSubscription;

        Flowable<Transaction> transactionFlowable = web3j.transactionFlowable();


        // Subscribe to transactions with retry logic and error handling
        disposableSubscription = transactionFlowable
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
                                .filter(tranReceiptOptional -> Utils.isERC20TokenCreation(transaction.getInput()))
                                .map(transactionReceipt -> new TransactionDetailsDTO(transaction, transactionReceipt.get()))
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
                    transactionDetailsDTOList.add(transactionDetailsDTO);

                    System.out.println( "New token discovered at: "  +  LocalDateTime.now().format(formatter) + " " + transactionDetailsDTO.getTransaction().getHash());
                }, throwable -> {
                    System.err.println("Error in Flowable subscription: " + throwable.getMessage());
                    throwable.printStackTrace();
                });
        return disposableSubscription;
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
