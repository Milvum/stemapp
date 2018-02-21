package com.milvum.stemapp;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.milvum.stemapp.geth.implementation.ContractInfo;
import com.milvum.stemapp.geth.implementation.VoteCaster;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.walletfactory.WalletFactory;
import com.milvum.stemapp.model.VoteCandidate;
import com.milvum.stemapp.model.exceptions.CreateWalletException;
import com.milvum.stemapp.model.exceptions.ReadWalletException;
import com.milvum.stemapp.utils.Utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

/**
 * .
 */

@RunWith(AndroidJUnit4.class)
public class VoteCasterTest {
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
    public void testRun_throwsNoException() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();

        Wallet wallet = WalletFactory.getInstance(context).load(WalletFactory.TEST_WALLET);
        // The candidate's address can be any valid ethereum address, as it only receives messages
        VoteCandidate candidate = new VoteCandidate("de Jong","0xaeeb3bdd666ff7abb516416b3698775d7cc04a05");

        VoteCaster voteCaster = new VoteCaster(context);
        voteCaster.run(wallet, candidate);
    }
}
