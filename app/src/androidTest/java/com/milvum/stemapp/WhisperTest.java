package com.milvum.stemapp;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.android.volley.Request;
import com.milvum.stemapp.ballotexchange.HttpRedeemClient;
import com.milvum.stemapp.model.Cipher;
import com.milvum.stemapp.model.MixingToken;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * .
 */

@RunWith(AndroidJUnit4.class)
public class WhisperTest {
    private static final String TAG = "WhisperTest";

    public static final String SIGN_ADDRESS = "http://10.0.2.2:8080/sign";
    public static final String VERIFY_ADDRESS = "http://10.0.2.2:8080/verify";

    // Some pre-generated test data.
    // The mixer signature is valid on this address + nonce combination.
    private static final String TEST_BALLOT_ADDRESS = "0x3b35cbb069d95f05cbeeb82a637628200e28ea5e";
    private static final BigInteger TEST_NONCE = BigInteger.valueOf(42);
    private static final String TEST_SIGNATURE_HEX = "0x7c569c7d7633198accbd387482191e855bdd4b7b5a45b10d6bc92173cf9a7aed9d016504e8ef1bdeb448b66ff1bf81172c11859bde486fda96a03d4e64d9c6b85a7707b806e13b195fe30adfcb93ebb943674f3a0b0b72946e954d3da7518ea98d99621fd2bd37b05e2c9c919fb940ec44e32ac84898de31ef797464e1bae564e193f2067677b1e28684a204e3ff70d4a13c9e0ecc220fedc414d37606488a91cc82981624eb16e08d8ce3bd68877f09d84e8dd5eeffb900bc882690b8c96dc2eec7abd89c5cecc02dac33cb57598c21e6d54975b7821b89a85a7320494c4872935e4d27567ee004562338a7894877510d5880f5b092b51d627a62df1754a91d";

    private HttpRedeemClient client;

    @Before
    public void setUp() {
        client = new HttpRedeemClient(InstrumentationRegistry.getTargetContext());
    }

    @After
    public void tearDown() {
        // tear down if necessary...
    }

    @Test
    public void testHttpRedeemRequest_successful() throws ExecutionException, InterruptedException {
        MixingToken token = new MixingToken(TEST_BALLOT_ADDRESS, TEST_NONCE);
        // Remove the "0x" at the start
        BigInteger signature = new BigInteger(TEST_SIGNATURE_HEX.substring(2), 16);

        Future<JSONObject> requestFuture = client.sendRedeemRequest(token, signature);
        // If an unsuccessful status code is returned (e.g. 4xx or 5xx), this throws an error
        requestFuture.get();
    }

    @Test(expected = ExecutionException.class)
    public void  testHttpRedeemRequest_unsuccesful() throws ExecutionException, InterruptedException {
        MixingToken token = new MixingToken(TEST_BALLOT_ADDRESS, TEST_NONCE);
        client.sendRedeemRequest(token, BigInteger.ONE)
            .get();
    }

    @Test
    public void testSignature() throws Exception {
        // create string
        String s = "0x857cc04e1583b35bf20b6acc622582ea24092095-12223";
        BigInteger modulus = new BigInteger("19149195346620339667138614708467591648473088784884031456565812848862672503151713897232053219423092451444497708751159482742218747391886718406696735456879151430685988442538422833461255824064149219421347867822968252104655370007707060632908914532101536230091111419656090447369198191610651971830578263737443475645600874865833428284745801329629414258370865891361205144768638956847575277397061350039526451218104528009164586144382206846968224033271358556846291532449069801088586442813760772728459924598939612858782684705191704575831558403250599740807308496545663915318691172272961584176035702617794352763164361777891398723211");
        BigInteger publicExponent = new BigInteger("65537");
        BigInteger privateKey = new BigInteger("10768043946305131113005773347134537055726974701150971379050305948077209358967455212394416242662911425806553154105413436951630185965686577558356268852592693710042735127834177589476901000720754553661823595417548087260827690478569789668501643762325677322543719260091031039823261073217073670108065684323603527899611416459199311857468338999502730929059210981437305224473996434580179385472736804325895307094223151917342364058304719262491882371624439391520935042476980170168931468440424789482666073539660908447980223433134475840032757372945342419820027322554846448701794506811202273404674924299621769990331320371213556585673");

        Cipher cipher = new Cipher(s, publicExponent, modulus);
        //Cipher cipher = new Cipher(BigInteger.ONE, publicExponent, modulus);
        BigInteger msg = Cipher.stringToBigInt(s);
        BigInteger blindedMsg = cipher.blind(msg);

        // sign (blinded and unblinded)
        String sig = client.sendRequest(SIGN_ADDRESS, Request.Method.POST, msg.toString(16))
                .get().getString("signature");
        String blindedSig  = client.sendRequest(SIGN_ADDRESS, Request.Method.POST, blindedMsg.toString(16))
                .get().getString("signature");

        String sig2 = msg.modPow(privateKey, modulus).toString(16);
        String blindedSig2 = blindedMsg.modPow(privateKey, modulus).toString(16);

        Assert.assertEquals("Local signing vs server signing", sig, sig2);
        Assert.assertEquals("Local blind signing vs server signing", blindedSig, blindedSig2);

        // unblind
        String unblinded  = cipher.unblind(new BigInteger(blindedSig, 16)).toString(16);

        // verify (send msg + unblinded) == success
        Boolean verified = client.sendRequest(VERIFY_ADDRESS, Request.Method.POST, s + "-" + unblinded)
                .get().getBoolean("result");

        boolean verified2 = cipher.verifyBlindedSignature(s, new BigInteger(blindedSig, 16));

        // verify unblinded == unblinded sig
        Assert.assertTrue("Local verification: ", verified2);
        Assert.assertEquals("Signature was not the same as unblinded (blinded sig)",
                sig, unblinded);
        Assert.assertTrue("Unblinded signature was not a proper signature", verified.booleanValue());
    }

    @Test
    public void testMinimalSignature() throws Exception {
        // Pre-knowledge
        BigInteger modulus = new BigInteger("19149195346620339667138614708467591648473088784884031456565812848862672503151713897232053219423092451444497708751159482742218747391886718406696735456879151430685988442538422833461255824064149219421347867822968252104655370007707060632908914532101536230091111419656090447369198191610651971830578263737443475645600874865833428284745801329629414258370865891361205144768638956847575277397061350039526451218104528009164586144382206846968224033271358556846291532449069801088586442813760772728459924598939612858782684705191704575831558403250599740807308496545663915318691172272961584176035702617794352763164361777891398723211");
        BigInteger publicExponent = new BigInteger("65537");

        // token generation
        String s = "0x857cc04e1583b35bf20b6acc622582ea24092095-51135";
        Cipher cipher = new Cipher(s, publicExponent, modulus);
        BigInteger blindedMsg = cipher.blind(Cipher.stringToBigInt(s));

        // signature
        String blindedSig  = client.sendRequest(SIGN_ADDRESS, Request.Method.POST, blindedMsg.toString(16))
                .get().getString("signature");

        // unblinding
        String unblinded = cipher.unblind(new BigInteger(blindedSig, 16)).toString(16);

        // verification
        Boolean verified = client.sendRequest(VERIFY_ADDRESS, Request.Method.POST,s + "-" + unblinded)
                .get().getBoolean("result");

        Assert.assertTrue("Unblinded signature was not a proper signature", verified.booleanValue());
    }


}
