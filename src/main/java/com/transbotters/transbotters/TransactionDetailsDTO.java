package com.transbotters.transbotters;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

@AllArgsConstructor
@Getter
public class TransactionDetailsDTO {
    Transaction transaction;
    TransactionReceipt transactionReceipt;

}
