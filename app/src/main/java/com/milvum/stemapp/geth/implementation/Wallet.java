package com.milvum.stemapp.geth.implementation;

import android.graphics.Bitmap;

import com.milvum.stemapp.BuildConfig;
import com.milvum.stemapp.model.ITransaction;
import com.milvum.stemapp.model.exceptions.SignTransactionException;

import org.ethereum.geth.Account;
import org.ethereum.geth.BigInt;
import org.ethereum.geth.KeyStore;
import org.ethereum.geth.Transaction;

/**
 * .
 *
 * Represents an Ethereum wallet (a.k.a address).
 * The wallet is encrypted with a password that is stored in this object.
 */
public class Wallet {
    private static final String TAG = "WalletUtil";

    // The keyStore is used to perform operations on/with the wallet
    private KeyStore keyStore;
    // The wallet effectively functions as a wrapper for the Account object
    private Account account;
    // We need the account's password to do any operations on it
    private String password;
    // If the wallet has been deleted, an error is thrown if you call any method on it
    private boolean deleted = false;

    // Should typically be constructed by using WalletFactory
    public Wallet(KeyStore keyStore, Account account, String password) {
        this.keyStore = keyStore;
        this.account = account;
        this.password = password;
    }

    // Make sure to call this at the start of every function
    private void assertState() {
        assert(!deleted);
    }

    // Delete the account from the keyStore and the file system
    // WARNING: do not call any more methods on this WalletUtil object after calling this function
    public void delete() throws Exception {
        assertState();

        keyStore.deleteAccount(account, password);
        deleted = true;

        android.util.Log.d(TAG, "Deleted wallet with address " + getAddressHex());
    }

    public String getAddressHex() {
        assertState();
        return account.getAddress().getHex();
    }

    public String getPassword() {
        assertState();
        return password;
    }

    // Directly get the underlying account
    // Not sure if this is preferred, or wrapping all relevant methods
    // To note: the account is not very useful without having access to the KeyStore
    public Account getAccount() {
        assertState();
        return account;
    }

    public org.web3j.abi.datatypes.Address getWeb3jAddress() {
        assertState();
        return new org.web3j.abi.datatypes.Address(getAddressHex());
    }

    public ITransaction signTransaction(ITransaction transaction) throws SignTransactionException {
        // Check types
        assert(transaction instanceof GethTransactionWrapper);

        try {
            Transaction signedTransaction = keyStore.signTxPassphrase(
                    account,
                    password,
                    ((GethTransactionWrapper) transaction).getTransaction(),
                    new BigInt(BuildConfig.CHAIN_ID));

            return new GethTransactionWrapper(signedTransaction);
        } catch (Exception e) {
            throw new SignTransactionException("Error while signing transaction", e);
        }
    }

    public Bitmap getQRCode() {
        return null;
    }

    //@TODO implement wrapper function for functionality that a WalletUtil should have
}
