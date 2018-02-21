package com.milvum.stemapp.utils;

import com.milvum.stemapp.geth.implementation.GethTransactionWrapper;
import com.milvum.stemapp.geth.implementation.HexValue;
import com.milvum.stemapp.geth.implementation.NonceGetter;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.Web3jProvider;
import com.milvum.stemapp.model.ITransaction;

import org.ethereum.geth.Address;
import org.ethereum.geth.BigInt;
import org.ethereum.geth.Geth;
import org.ethereum.geth.Transaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

import java.math.BigInteger;

import static java.lang.Thread.sleep;

/**
 * .
 */

public class TransactionUtil {
    private TransactionUtil() {

    }

    private static final String TAG = "TransactionUtil";

    public static void transferEther(
            Wallet from,
            String toAddress,
            BigInteger amount)
            throws Exception {

        // Some random transaction
        org.ethereum.geth.Transaction gethTransaction = Geth.newTransaction(
                NonceGetter.getNonce(from.getAddressHex()), // nonce
                new Address(toAddress), // to
                new BigInt(amount.longValue()), // amount
                new BigInt(Constants.LOAD_GAS_LIMIT), // gas limit
                new BigInt(Constants.LOAD_GAS_PRICE), // gas price
                null); // data
        ITransaction<Transaction> transaction = new GethTransactionWrapper(gethTransaction);

        // Sign transaction
        ITransaction<org.ethereum.geth.Transaction> signedTransaction =
                from.signTransaction(transaction);

        // Make web3 connection to remote node
        Web3j web3 = Web3jProvider.getWeb3j();

        // Get raw transaction
        HexValue rawHex = new HexValue(signedTransaction.getTransaction().encodeRLP());
        android.util.Log.d(TAG, "Transferring " + amount.toString() + " Ether from " +
                from.getAddressHex() + " to " + toAddress + " ...");

        EthSendTransaction ethSendTransaction =
                web3.ethSendRawTransaction(rawHex.toString()).send();
        String hash = ethSendTransaction.getTransactionHash();

        android.util.Log.d(TAG, "Transaction posted, waiting for receipt for hash " + hash + " ...");

        String receipt = null;

        while (web3.ethGetTransactionReceipt(hash).send().getTransactionReceipt() == null) {
            sleep(500);
        }

        android.util.Log.d(TAG, "Transfer done!");
    }
}
