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
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.*;

@Component
public class ScannerBot {
    private  DateTimeFormatter formatter;

    private final Web3j web3j;
    private final Disposable disposableSubPendingTxs;
    private final Disposable disposableSubSuccessTxs;


    public ScannerBot(RPCProvider rpcProvider) {
       initDateTimeFormatter();

        web3j = Web3j.build(new HttpService(rpcProvider.getALCHEMY_URL()));

        this.disposableSubPendingTxs = startDisposablePendingTxSubscriber();
        this.disposableSubSuccessTxs = startDisposableSuccessTxSubscriber();

    }


    /** !IMPORTANT: We try to find pending transactions.
     * The pending transactions are not common between nodes but each node has a pool of different pending transactions.
     * Each node "gossips" (or communicates) its pending transactions to other nodes, but they are not completely synced in real time.
     * Also, the submission time of a pending transaction is not known or stored on the blockchain,we can only know when WE have
     * discovered said transaction but not how long it has been pending.
     * @return
     */
    private @NotNull Disposable startDisposablePendingTxSubscriber() {
        final Disposable disposableSubscription;
        disposableSubscription = startDisposableTxSubscriber(ETransactionType.TOKEN_CREATION,ETransactionStatus.PENDING);
        return disposableSubscription;
    }

    private @NotNull Disposable startDisposableSuccessTxSubscriber() {
        final Disposable disposableSubscription;
        disposableSubscription = startDisposableTxSubscriber(ETransactionType.TOKEN_CREATION,ETransactionStatus.SUCCESS);
        return disposableSubscription;
    }



    private @NotNull Disposable startDisposableTxSubscriber(ETransactionType desiredTransactionType , ETransactionStatus desiredTransactionStatus) {
        final Disposable disposableSubscription;

        Flowable<Transaction> transactionFlowable = ETransactionStatus.PENDING == desiredTransactionStatus ?
                web3j.pendingTransactionFlowable() : web3j.transactionFlowable();

        // Subscribe to pending transactions with retry logic and error handling
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
                                .filter(tranReceiptOptional -> transactionFiltering(desiredTransactionType, desiredTransactionStatus, tranReceiptOptional, transaction)) //only mined transactions have a TransactionReceipt, so Empty Optional<TransactionReceipt>  means it's pending still
                                .map(transactionReceipt -> transaction)
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
                .subscribe(transaction -> {
                    System.out.println(desiredTransactionStatus.getMessage() + " discovered at: "  +  LocalDateTime.now().format(formatter) + " " + transaction.getHash());
                }, throwable -> {
                    System.err.println("Error in " + desiredTransactionStatus.getMessage() + " Flowable subscription: " + throwable.getMessage());
                    throwable.printStackTrace();
                });
        return disposableSubscription;
    }

    private boolean transactionFiltering(ETransactionType desiredTransactionType, ETransactionStatus desiredTransactionStatus, Optional<TransactionReceipt> transactionReceipt, Transaction currentTransaction) throws UnknownTransactionStatusException {
        if(ETransactionStatus.PENDING == desiredTransactionStatus){
            return switch (desiredTransactionType) {
                //only mined transactions have a TransactionReceipt, so Empty Optional<TransactionReceipt> means it's still pending
                // (what our connected node initially sees as pending is not guaranteed to be not mined yet by someone. Only mined transactions are guaranteed)
                case TOKEN_CREATION -> Utils.isERC20TokenCreation(currentTransaction.getInput()) && transactionReceipt.isEmpty();
                case ADD_LIQUIDITY -> true; //not implemented yet
                default -> true; //when no desiredTransactionType is defined, allow all
            };
        }
        else if(ETransactionStatus.SUCCESS == desiredTransactionStatus){
            return switch (desiredTransactionType) {
                case TOKEN_CREATION -> Utils.isERC20TokenCreation(currentTransaction.getInput());
                case ADD_LIQUIDITY -> true; //not implemented yet
                default -> true; //when no desiredTransactionType is defined, allow all
            };
        }
        throw new UnknownTransactionStatusException("Unhandled Transaction Status: " + desiredTransactionStatus);
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
        this.disposableSubPendingTxs.dispose();
        this.disposableSubSuccessTxs.dispose();
        this.web3j.shutdown();
    }
}
