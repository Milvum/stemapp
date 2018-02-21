package com.milvum.stemapp.geth.implementation;

import com.milvum.stemapp.model.exceptions.EthereumClientCreationException;
import com.milvum.stemapp.model.exceptions.SendTransactionException;

import org.ethereum.geth.EthereumClient;
import org.ethereum.geth.Geth;
import org.web3j.protocol.core.Ethereum;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * .
 */

public class NonceGetter {
    private static final String TAG = "NonceGetter";

    private static Map<String, BigInteger> nonces = new HashMap<String, BigInteger>();

    // Returns the next nonce that is available for transactions from the given hexadecimal address
    public static synchronized long getNonce(String addressHex)
            throws SendTransactionException, EthereumClientCreationException {

        if (!nonces.containsKey(addressHex)) {
            long freshNonce = getFreshNonce(addressHex);
            nonces.put(addressHex, BigInteger.valueOf(freshNonce));

            return freshNonce;
        }

        long nonce = nonces.get(addressHex).longValue() + 1L;
        nonces.put(addressHex, BigInteger.valueOf(nonce));

        android.util.Log.d(TAG, "Got nonce " + nonce + " for address " + addressHex);
        return nonce;
    }

    private static long getFreshNonce(String addressHex) throws SendTransactionException, EthereumClientCreationException {
        EthereumClient client = EthereumClientProvider.getEthereumClient();
        org.ethereum.geth.Context gethContext = Geth.newContext();

        try {
            long nonce = client.getPendingNonceAt(gethContext, Geth.newAddressFromHex(addressHex));

            android.util.Log.d(TAG, "Got FRESH nonce " + nonce + " for address " + addressHex);

            return nonce;
        } catch (Exception e) {
            throw new SendTransactionException("Error while getting a fresh nonce for address: " + addressHex, e);
        }
    }
}
