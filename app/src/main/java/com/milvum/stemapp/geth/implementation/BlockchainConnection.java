package com.milvum.stemapp.geth.implementation;

import android.content.Context;
import android.content.SharedPreferences;

import com.milvum.stemapp.model.ITransaction;
import com.milvum.stemapp.model.ITransactionPoster;
import com.milvum.stemapp.model.exceptions.EthereumClientCreationException;
import com.milvum.stemapp.model.exceptions.SendTransactionException;

import org.ethereum.geth.EthereumClient;
import org.ethereum.geth.Geth;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;

import java.math.BigInteger;
import java.util.List;

/**
 * .
 */

public final class BlockchainConnection implements ITransactionPoster {
    private static final String TAG = "BlockchainConnection";
    private static final String PREFS = "BlockchainConnection:Prefs";
    private static final String PREFS_BLOCK = "block";

    private static BlockchainConnection instance = null;

    private final EthereumClient client;

    private BlockchainConnection() throws EthereumClientCreationException {
        client = EthereumClientProvider.getEthereumClient();
    }

    // Get the singleton instance
    public static BlockchainConnection getInstance() throws EthereumClientCreationException {
        if (instance == null) {
            instance = new BlockchainConnection();
        }

        return instance;
    }

    /**
     * Stores the latest known block in SharedPreferences. If block is null it will remove the
     * entry from the SharedPreferences.
     * @param context Application Context
     * @param block Block number to store, or null.
     */
    public static void setLatestBlock(Context context, BigInteger block) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (block == null) {
            prefs.edit().remove(PREFS_BLOCK).apply();
        } else {
            String b = block.toString(10);

            prefs.edit().putString(PREFS_BLOCK, b).commit();
        }
    }

    /**
     * Gets the latest known block, if no block is known it will return LATEST.
     * @param context Application Context
     * @return Latest known block, or LATEST.
     */
    public static DefaultBlockParameter getLatestBlock(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String block = prefs.getString(PREFS_BLOCK, null);

        if (block == null) {
            return DefaultBlockParameterName.LATEST;
        } else {
            return new DefaultBlockParameterNumber(new BigInteger(block, 10));
        }
    }

    @Override
    public void postTransactions(List<ITransaction> transactions) throws SendTransactionException {
        // Check types
        for (ITransaction transaction : transactions) {
            assert(transaction instanceof GethTransactionWrapper);
        }

        org.ethereum.geth.Context gethContext = Geth.newContext();

        // Send transactions
        for (ITransaction transaction : transactions) {
            try {
                client.sendTransaction(gethContext, ((GethTransactionWrapper) transaction).getTransaction());
            } catch (Exception e) {
                throw new SendTransactionException("Error while posting transaction to blockchain", e);
            }
        }
    }
}
