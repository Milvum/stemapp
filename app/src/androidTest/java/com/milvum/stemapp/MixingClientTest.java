package com.milvum.stemapp;

import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.milvum.stemapp.ballotexchange.HttpRedeemClient;
import com.milvum.stemapp.contracts.BallotDispenser;
import com.milvum.stemapp.contracts.VotingPass;
import com.milvum.stemapp.geth.implementation.MixingClient;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.Web3jProvider;
import com.milvum.stemapp.geth.implementation.contract.VotingPassUtil;
import com.milvum.stemapp.geth.implementation.walletfactory.WalletFactory;
import com.milvum.stemapp.model.WalletRole;
import com.milvum.stemapp.model.exceptions.CreateWalletException;
import com.milvum.stemapp.model.exceptions.ReadWalletException;
import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.Utils;
import com.milvum.stemapp.utils.WalletUtil;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;

import cz.msebera.android.httpclient.client.methods.HttpPost;
import rx.Observable;
import rx.functions.Func1;
import rx.observables.BlockingObservable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * .
 */

@RunWith(AndroidJUnit4.class)
public class MixingClientTest {
    private Web3j web3j;
    private Context context;
    private Wallet passWallet;
    private Wallet ballotWallet;

    @Before
    public void setUp() throws Exception {
        web3j = Web3jProvider.getWeb3j();
        context = InstrumentationRegistry.getTargetContext();

        passWallet = WalletUtil.fromKey(context, WalletRole.PASS);
        ballotWallet = WalletUtil.fromKey(context, WalletRole.BALLOT);
    }

    private void givePass() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();

        HttpRedeemClient client = new HttpRedeemClient(context);
        client.beg(passWallet.getAddressHex());

        Observable<VotingPass.GiveEventResponse> observable = VotingPassUtil.getContract(context, passWallet)
                .giveEventObservable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST)
                .filter((giveEventResponse) -> giveEventResponse.to.equals(passWallet.getAddressHex()))
                .first();

        BlockingObservable.from(observable).single();

    }

    @Test
    public void URITest() {
        new HttpPost(BuildConfig.NODE_ADDRESS);
    }

    @Test
    public void testInitMixingClient() throws Exception {
        MixingClient client = MixingClient.getInstance(context);

        BallotDispenser dispenserContract = client.getBallotDispenser();

        String address = dispenserContract.mixer().send();

        assertNotNull(address);
    }

    // at this moment we cannot easily remove any added votingpasses,
    // thus we cannot guarantee that the account has 0 votingpass at the start of the test
    @Test @Ignore
    public void testRequestMixWithoutPass() throws Exception
    {
        MixingClient client = MixingClient.getInstance(context);

        // should fail due to no voting pass
        TransactionReceipt receipt = client.requestMix().get();

        // Fail ==  all gas used
        assertEquals(Constants.LOAD_GAS_LIMIT, receipt.getGasUsed().longValue());
    }

    @Test
    public void testRequestMixWithPass() throws Exception {
        MixingClient client = MixingClient.getInstance(context);
        givePass();

        TransactionReceipt receipt = client.requestMix().get();
        List<BallotDispenser.JoinRequestedEventResponse> events =
                client.getBallotDispenser().getJoinRequestedEvents(receipt);

        assertNotEquals("All gas used == error", BigInteger.valueOf(Constants.LOAD_GAS_LIMIT), receipt.getGasUsed());
        assertEquals(1, events.size());

        BallotDispenser.JoinRequestedEventResponse event = events.get(0);
        assertEquals(passWallet.getAddressHex(), event.client);
    }

    @Test
    public void testPayDeposit() throws Exception {
        MixingClient client = MixingClient.getInstance(context);

        // obtain balance
        String mixer = client.getBallotDispenser().mixer().send();
        BigInteger initialBalance = web3j.ethGetBalance(mixer, DefaultBlockParameterName.LATEST)
                .send()
                .getBalance();

        // action
        client.requestMix().get();
        TransactionReceipt receipt = client.payDeposit(context).get();

        assertNotEquals(BigInteger.valueOf(Constants.LOAD_GAS_LIMIT), receipt.getGasUsed());

        // compare final balance
        BigInteger finalBalance = web3j.ethGetBalance(mixer, DefaultBlockParameterName.LATEST)
                .send()
                .getBalance();

        // difference should be equal to deposit amount
        assertTrue(finalBalance.compareTo(initialBalance) == 1);
    }

}
