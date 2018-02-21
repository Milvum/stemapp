// Has its own package, because the constructor is protected to allow subclasses,
//   but should otherwise never be called for any other reason.
package com.milvum.stemapp.geth.implementation.walletfactory;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.model.exceptions.CreateWalletException;
import com.milvum.stemapp.model.exceptions.ReadWalletException;

import org.apache.commons.lang3.NotImplementedException;
import org.ethereum.geth.Account;
import org.ethereum.geth.Accounts;
import org.ethereum.geth.Geth;
import org.ethereum.geth.KeyStore;

import java.io.File;

/**
 * .
 *
 * Singleton factory that creates WalletUtil objects.
 * It is also responsible for the KeyStore object that all wallets share.
 */

public class WalletFactory {
    private static final String TAG = "WalletFactory";
    private static WalletFactory instance = null;

    // Relative from the app's files directory (from getFilesDir())
    public static final String WALLET_PATH = "/keystore/";
    public static final String DEFAULT_PASSWORD = "decide-fifty-control-myself";

    // A wallet that has Ether in it encrypted with the default password,
    //   used for testing and development purposes.
    // @TODO: Remove in production
//    public static final String TEST_WALLET = "0xf5587f68d63404a35d4811d29c26ff16ba3fa613";
    public static final String TEST_WALLET = "0x5ad91bf68720b9281824df87680c0db60ee843ef";
    // Can act as Pass issuer (no password)
    public static final String TEST_WALLET2 = "0xf5587f68d63404a35d4811d29c26ff16ba3fa613";

    private KeyStore keyStore;

    protected WalletFactory(Context context) {
        // Create an encrypted keystore with light crypto parameters
        keyStore = new KeyStore(context.getFilesDir() + WALLET_PATH ,
                Geth.LightScryptN, Geth.LightScryptP);

    }

    // Get the singleton instance
    public static WalletFactory getInstance(Context context) {
        if (instance == null) {
            instance = new WalletFactory(context);
        }

        return instance;
    }

    // Construction function for new wallet
    public Wallet makeNew(String password) {
        try {
            Account newAccount = keyStore.newAccount(password);
            return new Wallet(keyStore, newAccount, password);
        } catch (Exception e) {
            throw new RuntimeException("Can't create new wallet", e);
        }
    }

    // Construction function for new wallet
    // Shorthand version that uses the DEFAULT_PASSWORD
    public Wallet makeNew() {
        return makeNew(DEFAULT_PASSWORD);
    }

    // Construct a wallet that exists on disk
    public Wallet load(String addressHex, String password) {
        Accounts accounts = keyStore.getAccounts();

        android.util.Log.d(TAG, "Found " + accounts.size() + " saved wallets:\n");

        // Find the wallet with a matching address
        for (int i = 0; i < accounts.size(); i++) {
            try {
                Account account = accounts.get(i);
                String accountAddressHex = account.getAddress().getHex();

                android.util.Log.d(TAG, "WalletUtil " + i + " - " + accountAddressHex);

                if (accountAddressHex.equals(addressHex)) {
                    return new Wallet(keyStore, accounts.get(i), password);
                }
            } catch (Exception e) {
                throw new RuntimeException("Can't get wallet " + i + "/" + accounts.size(), e);
            }
        }

        // No wallet found\
        throw new RuntimeException("Cannot find wallet " + addressHex);
    }

    // Construct a wallet that exists on disk.
    // Shorthand version that uses the DEFAULT_PASSWORD
    public Wallet load(String addressHex)
            throws ReadWalletException {
        return load(addressHex, DEFAULT_PASSWORD);
    }

    // @TODO implement option to import a raw private key as a wallet
    public static Wallet fromPrivateKey(byte[] privateKey, String password)
            throws NotImplementedException {
        throw new NotImplementedException("TODO: implement private key import");
    }
}
