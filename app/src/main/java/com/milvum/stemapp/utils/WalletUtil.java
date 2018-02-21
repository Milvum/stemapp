package com.milvum.stemapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.milvum.stemapp.R;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.walletfactory.WalletFactory;
import com.milvum.stemapp.model.WalletRole;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * .
 */

public class WalletUtil {
    private static final String TAG = "WalletUtil";

    private static Map<String, Wallet> wallets = new HashMap<>();

    /**
     * Retrieve Wallet (pass or ballot) by looking through the preferences and obtaining parameters
     * for WalletFactory
     * @param context - Android Context
     * @param key - key to the hash of either pass or ballot wallet
     * @return - A wallet
     */
    static public Wallet fromKey(Context context, WalletRole role) {
        String key = role.getName();

        if(wallets.containsKey(key)) {
            return wallets.get(key);
        }

        SharedPreferences preferences = context.getSharedPreferences(
                context.getString(R.string.preferenceFile), Context.MODE_PRIVATE);

        Wallet wallet;
        if(preferences.contains(key)) {
            String walletHash = preferences.getString(key, null);
            String password = preferences.getString(walletHash, null);

            if(password == null) {
                throw new AssertionError("Wallet without password");
            }

            try {
                wallet = WalletFactory.getInstance(context).load(walletHash, password);
            } catch (Exception e) {
                wallet = createWallet(context, key, preferences);
            }
        } else {
            wallet = createWallet(context, key, preferences);
        }

        wallets.put(key, wallet);
        Log.d("Mixing", "Returned wallet " + key + ": " + wallet.getAddressHex());
        return wallet;
    }

    @NonNull
    private static Wallet createWallet(Context context, String key, SharedPreferences preferences) {;
        String password = UUID.randomUUID().toString();
        Wallet wallet = WalletFactory.getInstance(context).makeNew(password);
        String walletHash = wallet.getAddressHex();

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, walletHash);
        editor.putString(walletHash, password);
        editor.apply();

        return wallet;
    }

    /**
     * Get a web3j Credentails object that represents the same Ethereum wallet
     *   as the given Wallet object.
     * @param context
     * @param wallet
     * @return
     * @throws IOException
     * @throws CipherException
     */
    static public Credentials walletToCredentials(Context context, final Wallet wallet)
            throws IOException, CipherException {
        return WalletUtils.loadCredentials(
                wallet.getPassword(),
                getWalletFile(context, wallet.getAddressHex()));
    }

    /**
     * Get the file in which the wallet with the address is saved on the device.
     * @param context
     * @param walletAddressHex should be a hexadecimal string starting with "0x"
     * @return
     */
    static public File getWalletFile(Context context, final String walletAddressHex) {
        File walletDir = new File(context.getFilesDir(), "keystore");

        File[] walletFiles = walletDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(walletAddressHex.substring(2));
            }
        });

        if (walletFiles.length != 1) {
            Log.e(TAG, "Found " + walletFiles.length +
                    " files that match wallet address " + walletAddressHex +
                    "\nSomething very strange is happening...");
        }

        return walletFiles[0];
    }

    /**
     * Destroy traces of wallet (useful for staying anonymous)
     * @param context - Android context
     * @param key - key to the hash of either pass or ballot wallet
     *            (though likely it will be ballot wallet)
     */
    static public void destroyWallet(Context context, String key) {
        wallets.remove(key);

        SharedPreferences preferences = context.getSharedPreferences(
                context.getString(R.string.preferenceFile), Context.MODE_PRIVATE);

        String walletHash = preferences.getString(key, null);

        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        editor.remove(walletHash);
        editor.apply();

        // TODO: find wallet file with walletHash and remove it
    }

}
