package com.milvum.stemapp;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.walletfactory.WalletFactory;
import com.milvum.stemapp.model.exceptions.CreateWalletException;
import com.milvum.stemapp.model.exceptions.ReadWalletException;

/**
 * .
 */

@RunWith(AndroidJUnit4.class)
public class WalletTest {
    private static final String TAG = "WalletTest";

    private WalletFactory factory;

    @Before
    public void setUp() {
        // If multiple tests will make use of context, it can also be stored in a private variable
        Context context = InstrumentationRegistry.getTargetContext();

        factory = WalletFactory.getInstance(context);
    }

    @After
    public void tearDown() {
        // tear down if necessary...
    }

    @Test
    public void testMakeNew_default() throws CreateWalletException {
        Wallet newWallet = factory.makeNew();
        assertNotNull(newWallet);

        android.util.Log.d(TAG, "Made wallet " + newWallet.getAddressHex());
    }

    @Test
    public void testMakeNew_pass() throws CreateWalletException {
        final String PASSWORD = "myPass";

        // TODO: test whether the password is actually set to PASSWORD
        Wallet newWallet = factory.makeNew(PASSWORD);
        assertNotNull(newWallet);

        android.util.Log.d(TAG, "Made wallet " + newWallet.getAddressHex());
    }

    @Test
    public void testLoad_default()
            throws CreateWalletException, ReadWalletException {
        // First, make a new wallet
        Wallet newWallet = factory.makeNew();
        assertNotNull(newWallet);
        android.util.Log.d(TAG, "Made new wallet " + newWallet.getAddressHex());

        // Then try to load it
        Wallet loadedWallet = factory.load(newWallet.getAddressHex());
        android.util.Log.d(TAG, "Loaded wallet " + loadedWallet.getAddressHex());

        // Check if the addresses match (i.e. it is really the same account)
        assertEquals(loadedWallet.getAddressHex(), newWallet.getAddressHex());
    }

    @Test
    public void testLoad_pass()
            throws CreateWalletException, ReadWalletException {
        final String PASSWORD = "myPass";

        // First, make a new wallet
        // TODO: test whether the password is actually set to PASSWORD
        Wallet newWallet = factory.makeNew();
        assertNotNull(newWallet);

        android.util.Log.d(TAG, "Made new wallet " + newWallet.getAddressHex());

        // Then try to load it
        Wallet loadedWallet = factory.load(newWallet.getAddressHex());
        android.util.Log.d(TAG, "Loaded wallet " + loadedWallet.getAddressHex());

        // Check if the addresses match (i.e. it is really the same account)
        assertEquals(loadedWallet.getAddressHex(), newWallet.getAddressHex());
    }

    @Test
    public void multipleFactories_loading_works()
            throws CreateWalletException, ReadWalletException {
        Context context = InstrumentationRegistry.getTargetContext();

        // Get a second wallet factory instance.
        // If the instances are not the same, new wallets will not be found by the instance.
        WalletFactory factory2 = WalletFactory.getInstance(context);

        // Make wallet in the factory #2 (because we already tested creation in factory #1)
        Wallet fac2Wallet = factory2.makeNew();
        assertNotNull(fac2Wallet);

        android.util.Log.d(TAG, "Made new wallet " + fac2Wallet.getAddressHex() + " in factory 2");

        // Then try to load it in factory #1
        Wallet loadedWallet = factory.load(fac2Wallet.getAddressHex());
        android.util.Log.d(TAG, "Loaded wallet " + loadedWallet.getAddressHex() + " in factory 1");

        // Check if the addresses match (i.e. it is really the same account)
        assertEquals(loadedWallet.getAddressHex(), fac2Wallet.getAddressHex());
    }
}
