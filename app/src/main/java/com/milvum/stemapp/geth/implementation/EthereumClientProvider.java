package com.milvum.stemapp.geth.implementation;

import com.milvum.stemapp.BuildConfig;
import com.milvum.stemapp.model.exceptions.EthereumClientCreationException;
import com.milvum.stemapp.model.exceptions.SignTransactionException;

import org.ethereum.geth.EthereumClient;
import org.ethereum.geth.Geth;

/**
 * .
 */

public class EthereumClientProvider {
    private static EthereumClient clientInstance = null;

    public static EthereumClient getEthereumClient() throws EthereumClientCreationException {
        if (clientInstance == null) {
            try {
                clientInstance = Geth.newEthereumClient(BuildConfig.NODE_ADDRESS);
            } catch (Exception e) {
                throw new EthereumClientCreationException("Error while signing transaction", e);
            }
        }

        return clientInstance;
    }
}
