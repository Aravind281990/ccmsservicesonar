package com.ccms.service.model;

import com.ccms.service.model.Transaction.TransactionDetail;

/**
 * Represents a transaction associated with a specific credit card.
 * <p>
 * This class encapsulates a single transaction and associates it with
 * a specific credit card identified by its `creditCardId`. The transaction 
 * details are contained in a `TransactionDetail` object, which provides 
 * further information about the transaction.
 * </p>
 */

public class TransactionWithCardId {
   
	private int creditCardId;
	private TransactionDetail transactionDetail;

    // Constructor
    public TransactionWithCardId(int creditCardId, TransactionDetail transactionDetail) {
        this.creditCardId = creditCardId;
        this.transactionDetail = transactionDetail;
    }

	// Getters and Setters
    public int getCreditCardId() {
        return creditCardId;
    }

    public void setCreditCardId(int creditCardId) {
        this.creditCardId = creditCardId;
    }

    public TransactionDetail getTransactionDetail() {
        return transactionDetail;
    }

    public void setTransactionDetail(TransactionDetail transactionDetail) {
        this.transactionDetail = transactionDetail;
    }
}
