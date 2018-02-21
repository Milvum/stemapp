package com.milvum.stemapp;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.milvum.stemapp.geth.implementation.ContractInfo;
import com.milvum.stemapp.geth.implementation.walletfactory.WalletFactory;
import com.milvum.stemapp.utils.Utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * .
 */

@RunWith(AndroidJUnit4.class)
public class UtilsTest {
    private static final String TAG = "UtilsTest";

    @Before
    public void setUp() {
        // set up if necessary..
    }

    @After
    public void tearDown() {
        // tear down if necessary...
    }

    @Test
    public void testGetContractInfo_addressesNotEmpty() {
        Context context = InstrumentationRegistry.getTargetContext();

        ContractInfo info = Utils.getContractInfo(context);

        assertNotNull(info.votingPassAddressHex);
        assertNotEquals(info.votingPassAddressHex, "");
        assertNotNull(info.ballotDispenserAddressHex);
        assertNotEquals(info.ballotDispenserAddressHex, "");
        assertNotNull(info.votingBallotAddressHex);
        assertNotEquals(info.votingBallotAddressHex, "");
    }
}
