package com.milvum.stemapp.model;

import com.milvum.stemapp.model.exceptions.SendTransactionException;

import java.util.List;

/**
 * .
 */

public interface ITransactionPoster {
    // TODO @jlicht Implement some failure mode wrt job queues
    public void postTransactions(List<ITransaction> transactions) throws SendTransactionException;
}
