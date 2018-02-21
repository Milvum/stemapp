package org.web3j.tx;

import com.milvum.stemapp.geth.implementation.AddressValue;
import com.milvum.stemapp.geth.implementation.GethTransactionWrapper;
import com.milvum.stemapp.geth.implementation.HexValue;
import com.milvum.stemapp.geth.implementation.NonceGetter;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.model.ITransaction;
import com.milvum.stemapp.model.exceptions.EthereumClientCreationException;
import com.milvum.stemapp.model.exceptions.SendTransactionException;

import org.ethereum.geth.BigInt;
import org.ethereum.geth.Geth;
import org.ethereum.geth.Transaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Async;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * .
 *
 * Similar to org.web3j.tx.RawTransactionManager, but with WalletUtil instead of Credentials as authentication object.
 * Uses geth transactions and signing because this is how WalletUtil is implemented.
 */

public class WalletTransactionManager extends TransactionManager {
    private static String TAG = "WalletTransactionMgr";

    private final Web3j web3j;
    final Wallet wallet;

    public WalletTransactionManager(Web3j web3j, Wallet wallet) {
        super(web3j);
        this.web3j = web3j;
        this.wallet = wallet;
    }

    public WalletTransactionManager(
            Web3j web3j, Wallet wallet, byte chainId, int attempts, int sleepDuration) {
        super(web3j, attempts, sleepDuration);
        this.web3j = web3j;
        this.wallet = wallet;
    }

    long getNonce() throws IOException {
        try {
            return NonceGetter.getNonce(wallet.getAddressHex());
        } catch (SendTransactionException e) {
            throw new IOException("Error getting nonce", e);
        } catch (EthereumClientCreationException e) {
            throw new IOException("Error getting nonce", e);
        }
    }

    /**
     * Creates an asynchronous transaction which will have been finished once the transactionReceipt
     * is returned
     * @return A promise of an TransactionReceipt
     */
    public Future<TransactionReceipt> asyncTransaction(
            final BigInteger gasPrice, final BigInteger gasLimit, final String to,
            final String data, final BigInteger value) {
        return Async.run(new Callable<TransactionReceipt>() {
            @Override
            public TransactionReceipt call() throws Exception {
                return executeTransaction(gasPrice, gasLimit, to, data, value);
            }
        });
    }

    @Override
    public EthSendTransaction sendTransaction(
            BigInteger gasPrice, BigInteger gasLimit, String to,
            String data, BigInteger value) throws IOException {

        long nonce = getNonce();
        AddressValue toValue = new AddressValue(to);
        HexValue dataValue = new HexValue(data);

        Transaction gethTransaction = Geth.newTransaction(
                nonce, // nonce
                toValue.toGethAddress(), // to
                new BigInt(value.longValue()), // amount
                new BigInt(gasLimit.longValue()), // gas limit
                new BigInt(gasPrice.longValue()), // gas price
                dataValue.toBytes()); // data

        return signAndSend(gethTransaction);
    }

    public EthSendTransaction signAndSend(Transaction gethTransaction)
            throws IOException {
        ITransaction<Transaction> transaction = new GethTransactionWrapper(gethTransaction);
        HexValue rawTransaction = null;

        try {
            // Sign transaction
            ITransaction<Transaction> signedTransaction = wallet.signTransaction(transaction);

            // Get raw transaction
            rawTransaction = new HexValue(signedTransaction.getTransaction().encodeRLP());
            EthSendTransaction result = web3j.ethSendRawTransaction(rawTransaction.toString()).sendAsync().get();

            return result;
        } catch (Exception e) {
            throw new IOException("Failed to signAndSend transaction", e);
        }
    }

    @Override
    public String getFromAddress() {
        return wallet.getAddressHex();
    }
}

