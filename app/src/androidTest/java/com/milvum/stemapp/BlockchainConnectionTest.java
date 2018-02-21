package com.milvum.stemapp;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.milvum.stemapp.geth.implementation.BlockchainConnection;
import com.milvum.stemapp.geth.implementation.GethTransactionWrapper;
import com.milvum.stemapp.geth.implementation.NonceGetter;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.walletfactory.WalletFactory;
import com.milvum.stemapp.model.ITransaction;
import com.milvum.stemapp.model.ITransactionPoster;
import com.milvum.stemapp.model.exceptions.CreateWalletException;
import com.milvum.stemapp.model.exceptions.EthereumClientCreationException;
import com.milvum.stemapp.model.exceptions.ReadWalletException;
import com.milvum.stemapp.model.exceptions.SendTransactionException;
import com.milvum.stemapp.model.exceptions.SignTransactionException;

import org.ethereum.geth.Address;
import org.ethereum.geth.BigInt;
import org.ethereum.geth.Geth;
import org.ethereum.geth.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * .
 */

@RunWith(AndroidJUnit4.class)
public class BlockchainConnectionTest {

    private static final String TAG = "BlockchainConnectionTst";

    private ITransactionPoster poster = null;

    @Before
    public void setUp() throws EthereumClientCreationException {
        poster = BlockchainConnection.getInstance();
    }

    @After
    public void tearDown() {
        // tear down if necessary...
    }

    @Test(expected = SendTransactionException.class)
    public void testPostTransactions_unsigned() throws SendTransactionException {

        // Some random empty transaction
        Transaction gethTransaction = new Transaction(
                1,
                new Address("0x0000000000000000000000000000000000000000"),
                new BigInt(0),
                new BigInt(10),
                new BigInt(1),
                null);
        ITransaction<Transaction> transaction = new GethTransactionWrapper(gethTransaction);

        List<ITransaction> transactions = new ArrayList<ITransaction>();
        transactions.add(transaction);

        // Send unsigned transaction should give "invalid sender" error
        poster.postTransactions(transactions);
    }

    @Test @Ignore
    // Also tests integration with WalletUtil
    public void testPostTransactions_singed() throws
            SendTransactionException,
            CreateWalletException,
            SignTransactionException,
            ReadWalletException,
            EthereumClientCreationException {

        // Load wallet with money (IMPORTANT), to sign with
        WalletFactory walletFactory = WalletFactory.getInstance(InstrumentationRegistry.getTargetContext());
        Wallet wallet = walletFactory.load(WalletFactory.TEST_WALLET);

        // Some random transaction
        Transaction gethTransaction = Geth.newTransaction(
                NonceGetter.getNonce(wallet.getAddressHex()), // nonce
                new Address("0x0000000000000000000000000000000000000000"), // to
                new BigInt(100000), // amount
                new BigInt(100000), // gas limit
                new BigInt(40000000000L), // gas price
                null); // data
        ITransaction<Transaction> transaction = new GethTransactionWrapper(gethTransaction);

        // Sign transaction
        ITransaction<Transaction> signedTransaction = wallet.signTransaction(transaction);

        List<ITransaction> transactions = new ArrayList<ITransaction>();
        transactions.add(signedTransaction);

        // Sending this singed transaction should work
        poster.postTransactions(transactions);
    }
}
