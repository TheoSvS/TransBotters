package com.transbotters.transbotters;

/**
 * The status of the transaction
 */
public enum ETransactionStatus {
    PENDING("PendingTx"),
    SUCCESS("SuccessTx");

    private final String message;

    ETransactionStatus(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
