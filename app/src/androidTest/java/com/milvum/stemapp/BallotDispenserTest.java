package com.milvum.stemapp;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.milvum.stemapp.contracts.BallotDispenser;
import com.milvum.stemapp.geth.implementation.AddressValue;
import com.milvum.stemapp.geth.implementation.MixingObserver;
import com.milvum.stemapp.geth.implementation.Wallet;

import org.junit.Ignore;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tx.WalletTransactionManager;
import com.milvum.stemapp.geth.implementation.Web3jProvider;
import com.milvum.stemapp.geth.implementation.walletfactory.WalletFactory;
import com.milvum.stemapp.model.exceptions.ReadWalletException;
import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.Utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import rx.Observable;
import rx.observers.TestSubscriber;


/**
 * .
 */

@RunWith(AndroidJUnit4.class)
public class BallotDispenserTest {
    private static final long GAS_LIMIT = 2000000L;

    // The wallets that will play their part in these tests
    // They need to be usable by this device
    private static final AddressValue ownerAddress = new AddressValue(WalletFactory.TEST_WALLET);
    private static final AddressValue passAddress = new AddressValue(WalletFactory.TEST_WALLET);

    private Context context;
    private Web3j web3j;

    private Wallet owner;
    private BallotDispenser ballotDispenser;

    @Before
    // @TODO: this only works if the TEST_WALLET is address owner. Would be nice if also worked otherwise.
    // If BallotDispenser.deploy() worked (it doesn't),
    //   we could deploy a separate test-BallotDispenser and set its owner to TEST_WALLET on deploy.
    public void setUp() throws ReadWalletException, ExecutionException, InterruptedException {
        context = InstrumentationRegistry.getTargetContext();
        web3j = Web3jProvider.getWeb3j();

        owner = WalletFactory.getInstance(context).load(ownerAddress.toString());

        ballotDispenser = BallotDispenser.load(Utils.getContractInfo(context).ballotDispenserAddressHex,
                web3j,
                new WalletTransactionManager(web3j, owner),
                Transaction.DEFAULT_GAS,
                BigInteger.valueOf(GAS_LIMIT));

        // Start the mix
        startMix().get();
    }

    private Future<TransactionReceipt> startMix() {
        return ballotDispenser.startMix(
                new BigInteger("7"),
                new BigInteger("10000"),
                new BigInteger("20000"),
                new BigInteger("30000"),
                new BigInteger("40000"),
                new BigInteger("0")).sendAsync();
    }

    private <T> T getOneEvent(List <T> events) {
        Assert.assertEquals(1, events.size());

        return events.get(0);
    }

    @Test
    @Ignore // TODO: currently fails since mix is not intialized
    // Subscribe to a JoinAccepted event relevant for the given client (address)
    // Should get exactly one event trigger, with the expected parameters
    public void testJoinAccepted_relevant() throws Exception {
        MixingObserver observer =
                new MixingObserver(new AddressValue(ballotDispenser.getContractAddress()));

        Observable<BallotDispenser.JoinAcceptedEventResponse> observable =
                observer.getJoinAcceptedObservable(passAddress, DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST);

        // Java's Observable testing tools
        TestSubscriber<BallotDispenser.JoinAcceptedEventResponse> testSubscriber =
                new TestSubscriber<>();
        observable.subscribe(testSubscriber);

        // Trigger a relevant JoinAccepted event (i.e. concerning the passAddress)
        final String PARTIAL_WARRANTY = "Fake Partial Warranty";
        TransactionReceipt receipt = ballotDispenser.acceptJoin(
                passAddress.toString(),
                PARTIAL_WARRANTY,
                false).send();

        // Check for errors, check for a single event trigger
        Assert.assertNotEquals("All gas == errors",
                BigInteger.valueOf(Constants.LOAD_GAS_LIMIT), receipt.getGasUsed());
        testSubscriber.assertNoErrors();
        List<BallotDispenser.JoinAcceptedEventResponse> events = testSubscriber.getOnNextEvents();
        BallotDispenser.JoinAcceptedEventResponse event = getOneEvent(events);

        // Check expected attributes of the event
        Assert.assertEquals(passAddress.toString(), event.client.toString());
        Assert.assertEquals(PARTIAL_WARRANTY, event.partialWarranty);
    }
}
