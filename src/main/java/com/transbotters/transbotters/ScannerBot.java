package com.transbotters.transbotters;

import com.transbotters.transbotters.block.BlockService;
import com.transbotters.transbotters.token.TokenService;
import com.transbotters.transbotters.web3.Web3Provider;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.utils.Convert;
//
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScannerBot {

    private final Web3Provider web3Provider;
    private final BlockService blockService;
    private final TokenService tokenService;

    private Disposable disposableSubSuccessTxs;

    Map<String, TransactionDetailsDTO> transactionDetailsMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void startScannerBot() {
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
                                .filter(tranDetailsOptional -> filterTransactionByTransactionType(tranDetailsOptional))
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
        if (Utils.isERC20TokenCreation(transactionDetailsDTO.getTransaction().getInput())) {
            handleTokenCreationTransaction(transactionDetailsDTO);
        } else if (Utils.isERC20AddLiquidity(transactionDetailsDTO.getTransaction().getInput())) {
            handleAddLiquidityTransaction(transactionDetailsDTO);
        } else if (isTokenBought(transactionDetailsDTO)) {
            handleTokenBoughtTransaction(transactionDetailsDTO);
        } else if (isLiquidityRemoved(transactionDetailsDTO)) {
            handleRemoveLiquidityTransaction(transactionDetailsDTO);
        }
    }

    private void handleRemoveLiquidityTransaction(TransactionDetailsDTO transactionDetailsDTO) {
        String tokenAddress = transactionDetailsDTO.getTransactionReceipt().getContractAddress();
        if (Objects.nonNull(tokenAddress) && !transactionDetailsMap.isEmpty() && transactionDetailsMap.containsKey(tokenAddress)) {
            log.info("Found transaction for remove liquidity from token transaction  https://etherscan.io/tx/" + transactionDetailsDTO.getTransaction().getHash());
            //TODO should remove token if liquidity removed
            //transactionDetailsMap.remove(tokenAddress);
        }
    }

    private void handleTokenBoughtTransaction(TransactionDetailsDTO transactionDetailsDTO) {
        //TODO contract address could be null so token address should be found
        //String tokenAddress = transactionDetailsDTO.getTransactionReceipt().getContractAddress();
        log.info("Found transaction for transfer to token with transaction  https://etherscan.io/tx/" + transactionDetailsDTO.getTransaction().getHash());
        log.info("Token" + " bought for: " + Convert.fromWei(transactionDetailsDTO.getTransaction().getValue().toString(), Convert.Unit.ETHER) + " ETH"); // Convert to ETH if needed
    }

    private boolean filterTransactionByTransactionType(TransactionDetailsDTO transactionDetails) {
        return Utils.isERC20TokenCreation(transactionDetails.getTransaction().getInput()) || //creation
                Utils.isERC20AddLiquidity(transactionDetails.getTransaction().getInput()) || //add liquidity
                isTokenBought(transactionDetails) || //token bought
                isLiquidityRemoved(transactionDetails); // remove liquidity
    }

    private boolean isLiquidityRemoved(TransactionDetailsDTO transactionDetailsDTO) {
        String createdTokenAddress = Utils.getCreatedTokenAddress(transactionDetailsDTO);
        return !transactionDetailsMap.isEmpty() &&
                Utils.isERC20RemoveLiquidity(transactionDetailsDTO.getTransaction().getInput())
                && transactionDetailsMap.containsKey(createdTokenAddress);
    }

    private void handleTokenCreationTransaction(TransactionDetailsDTO transactionDetailsDTO) {
        String createdTokenAddress = Utils.getCreatedTokenAddress(transactionDetailsDTO);
        var honeyPot = Utils.getSecurityResponse(createdTokenAddress);
        if (honeyPot) {
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
                        log.getTopics().size() >= 3 && transactionDetailsMap.containsKey(log.getTopics().get(2)));
        boolean isValueAboveZero = transactionDetails.getTransaction().getValue().compareTo(BigInteger.ZERO) > 0;
        return isNotEmpty && isTransfer && isToAddress && isValueAboveZero;
    }

    private void handleAddLiquidityTransaction(TransactionDetailsDTO transactionDetailsDTO) {
        // Hybrid detection: try signature first, then events as fallback
        Optional<String> detectedTokenWithLiquidityOpt =
                detectAddedLiquidityTokenBySignature(transactionDetailsDTO).or(() ->
                        detectAddedLiquidityTokenByEvents(transactionDetailsDTO));

        if (detectedTokenWithLiquidityOpt.isPresent()) {
            String detectedLiquidToken = detectedTokenWithLiquidityOpt.get();
            if (transactionDetailsMap.containsKey(detectedTokenWithLiquidityOpt.get())) {
                log.info("🚀 Liquidity added for TRACKED token: " + detectedLiquidToken);
                log.info("   TX: " + web3Provider.getChainExplorerURL() + "/tx/" + transactionDetailsDTO.getTransaction().getHash());
                log.info("   Token: " + web3Provider.getChainExplorerURL() + "/token/" + detectedLiquidToken);

                // Log ETH amount if present
                BigInteger ethValue = transactionDetailsDTO.getTransaction().getValue();
                if (ethValue.compareTo(BigInteger.ZERO) > 0) {
                    log.info("   ETH: " + Convert.fromWei(ethValue.toString(), Convert.Unit.ETHER) + " ETH");
                }
            } else {
                log.info("Liquidity added (not tracked or token not resolved): " + transactionDetailsDTO.getTransaction().getHash());
                log.info("   Token: " + web3Provider.getChainExplorerURL() + "/token/" + detectedLiquidToken);
            }
        }
    }

    /**
     * Signature-based detection: parse input data of known addLiquidity functions
     * and try to extract token addresses. Returns the tracked token if any match.
     */
    private Optional<String> detectAddedLiquidityTokenBySignature(TransactionDetailsDTO dto) {
        String input = dto.getTransaction().getInput();
        if (input == null || !input.startsWith("0x") || input.length() < 10) return Optional.empty();

        String selector = input.substring(2, 10).toLowerCase();

        // Helper to extract 32-byte word at index i (0-based after selector)
        Function<Integer, String> wordAt = i -> {
            int start = 10 + (i * 64);
            int end = Math.min(input.length(), start + 64);
            if (end - start != 64) return null;
            return input.substring(start, end);
        };

        // Helper: convert a 32-byte ABI word to address (last 40 hex chars)
        Function<String, String> wordToAddress = w -> w == null ? null : ("0x" + w.substring(24)).toLowerCase();

        switch (selector) {
            case Utils.ERC20_ADD_LIQUIDITY_FUNCTION_SIGNATURE -> {
                String a0 = wordToAddress.apply(wordAt.apply(0));
                String a1 = wordToAddress.apply(wordAt.apply(1));
                return a0 != null ? Optional.of(a0) : Optional.ofNullable(a1);
            }
            case Utils.ERC20_ADD_LIQUIDITY_ETH_FUNCTION_SIGNATURE -> {
                String token = wordToAddress.apply(wordAt.apply(0));
                return Optional.ofNullable(token);
            }
        }
        // BalancerV2 joinPool(bytes32, address, address, ...) -> b04e41c3 (hard to parse tokens)
        // Fallback: not parsing complex aggregator inputs here.
        // Event logs method will handle them instead
        return Optional.empty();
    }

    /**
     * Event-based detection: use Mint and Transfer logs to infer which token received liquidity.
     * Strategy:
     * 1) Find pair address from Mint event (emitted by UniswapV2Pair).
     * 2) Find Transfer events where 'to' equals that pair address.
     * The log.address of those events are token contracts. Intersect with tracked tokens.
     */
    private Optional<String> detectAddedLiquidityTokenByEvents(TransactionDetailsDTO dto) {
        List<Log> logs = dto.getTransactionReceipt().getLogs();
        if (CollectionUtils.isEmpty(logs)) return Optional.empty();

        // 1) Find pair address via Mint event
        String pairAddress = null;
        for (Log log : logs) {
            if (CollectionUtils.isEmpty(log.getTopics())) continue;
            String topic0 = log.getTopics().get(0);
            if (topic0 != null && topic0.equals("0x" + Utils.UNISWAP_V2_MINT_EVENT)) {
                pairAddress = log.getAddress();
                break;
            }
        }
        if (pairAddress == null) return Optional.empty();

        // 2) Find Transfer events to the pair address; the emitting contract is the token address
        String zeroPadPair = pairAddress.toLowerCase().replace("0x", "");
        zeroPadPair = String.format("%064s", zeroPadPair).replace(' ', '0');
        String encodedToTopic = "0x" + zeroPadPair; // topic[2]

        for (Log log : logs) {
            if (CollectionUtils.isEmpty(log.getTopics()) || log.getTopics().size() < 3) continue;
            String topic0 = log.getTopics().get(0);
            String toTopic = log.getTopics().get(2);
            String tokenAddress = log.getAddress().toLowerCase();
            // ERC20 Transfer topic
            if (("0x" + Utils.ERC20_TRANSFER_TOPIC).equals(topic0)
                    && encodedToTopic.equalsIgnoreCase(toTopic)) {
                return Optional.of(tokenAddress);
            }
        }

        return Optional.empty();
    }

    @PreDestroy
    public void cleanup() {
        this.disposableSubSuccessTxs.dispose();
    }
}
