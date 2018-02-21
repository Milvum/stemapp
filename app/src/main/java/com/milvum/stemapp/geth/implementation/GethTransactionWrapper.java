package com.milvum.stemapp.geth.implementation;

import com.milvum.stemapp.model.ITransaction;

import org.ethereum.geth.Transaction;

/**
 * .
 */

public class GethTransactionWrapper implements ITransaction<Transaction> {
    private Transaction transaction;

    public GethTransactionWrapper(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public Transaction getTransaction() {
        return transaction;
    }
}
