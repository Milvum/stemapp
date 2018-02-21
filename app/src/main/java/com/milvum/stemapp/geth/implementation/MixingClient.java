package com.milvum.stemapp.geth.implementation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.milvum.stemapp.BuildConfig;
import com.milvum.stemapp.ballotexchange.HttpRedeemClient;
import com.milvum.stemapp.contracts.BallotDispenser;
import com.milvum.stemapp.model.Cipher;
import com.milvum.stemapp.model.MixingParameters;
import com.milvum.stemapp.model.MixingToken;
import com.milvum.stemapp.model.WalletRole;
import com.milvum.stemapp.model.exceptions.EthereumClientCreationException;
import com.milvum.stemapp.model.exceptions.SendTransactionException;
import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.Utils;
import com.milvum.stemapp.utils.WalletUtil;

import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple8;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.WalletTransactionManager;
import org.web3j.utils.Async;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * .
 * Manages the interactions with the BallotDispenser contract to exchange a Voting Pass
 * for a Voting Ballot.
 */

public class MixingClient {
    private static final String TAG = "MixingClient";
    private static final String PREFS = "MIXING_CLIENT_PREFS";
    private static final String MIXING_PARAMETER_STORE = "MIXING_PARAMETER_STORE";
    private static final String MIXING_TOKEN_STORE = "MIXING_TOKEN_STORE";
    private static final String MIXING_CIPHER_STORE = "MIXING_CIPHER_STORE";
    private static MixingClient instance;

    private final BallotDispenser ballotDispenser;
    private final Wallet passWallet;
    private final Wallet ballotWallet;
    private final Context context;

    private MixingParameters mixingParameters = null;
    private MixingToken mixingToken = null;
    private Cipher cipher = null;

    private MixingClient(Context context, Wallet passWallet, Wallet ballotWallet) {
        this.context = context;
        String contractAddress = Utils.getContractInfo(context).ballotDispenserAddressHex;
        Web3j web3j = Web3jProvider.getWeb3j();
        TransactionManager manager = new WalletTransactionManager(web3j, passWallet);

        this.ballotWallet = ballotWallet;
        this.passWallet = passWallet;
        this.ballotDispenser = BallotDispenser.load(
                contractAddress,
                web3j,
                manager,
                BigInteger.valueOf(Constants.LOAD_GAS_PRICE),
                BigInteger.valueOf(Constants.LOAD_GAS_LIMIT));
    }

    public BallotDispenser getBallotDispenser() {
        return ballotDispenser;
    }

    /**
     * Request to join the current (pending) mix. This follows the mix parameters that are currently
     * present in the BallotDispenser contract.
     *
     * @throws InterruptedException - problems with retrieving info of BallotDispenser
     * @throws ExecutionException   - problems with retrieving info of BallotDispenser
     */
    public Future<TransactionReceipt> requestMix() throws Exception {
        mixingToken = new MixingToken(
                ballotWallet.getAddressHex(),
                new BigInteger(16, new Random()));

        Future<Tuple8<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, Boolean>>
                parametersTask = ballotDispenser.currentMix().sendAsync();

        Future<BigInteger> exponentTask = ballotDispenser.publicKey().sendAsync();
        Future<String> modulusTask = ballotDispenser.moduloParam().sendAsync();

        BigInteger publicExponent = exponentTask.get();
        String modulusString = modulusTask.get();
        BigInteger modulus = new BigInteger(modulusString.substring(2), 16);

        String tokenString = mixingToken.toString();
        cipher = new Cipher(tokenString, publicExponent, modulus);
        BigInteger blinded = cipher.blind(Cipher.stringToBigInt(tokenString));

        String mixingToken = blinded.toString(16);

        mixingParameters = new MixingParameters(parametersTask.get());

        storeMixingState(this);

        return ballotDispenser.joinMix(
                mixingParameters.deposit,
                mixingParameters.deadlineTransferClient,
                mixingParameters.deadlineProvideWarranty,
                mixingParameters.deadlineUnblindAddress,
                mixingParameters.deadlineTransferMixer,
                mixingParameters.minimumBlockAmount,
                mixingToken).sendAsync();
    }

    /**
     * Pay the deposit price for participating in the mix
     */
    public Future<TransactionReceipt> payDeposit(final Context context)
            throws EthereumClientCreationException, SendTransactionException {
        if (mixingParameters == null)
            throw new AssertionError("Mixing was not initiated");

        return ballotDispenser.payMixer(mixingParameters.deposit).sendAsync();
    }

    /**
     * !!! The Stemapp framework does not (yet) make use of Whisper. !!!
     * !!! This function uses an HTTP POST request as a placeholder. !!!
     * Whisper the signed MixingToken. The Mixer can listen for this whisper and verify the signed
     * token.
     */
    public Future<JSONObject> whisperAddress(Context context, BigInteger blindedSignature) {
        BigInteger signature = cipher.unblind(blindedSignature);

        if (!cipher.verifyBlindedSignature(mixingToken.toString(), blindedSignature)) {
            Log.e("Mixing", "blinded signature is inccorect");
        } else Log.v("Mixing", "Signature is correct according to our parameters");


        HttpRedeemClient client = new HttpRedeemClient(context);
        return client.sendRedeemRequest(mixingToken, signature);
    }

    private static byte[] serializeObject(Serializable object) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);

            return baos.toByteArray();
        } catch (IOException ex) {
            Log.e("Mixing", "Failed to serialize object", ex);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Serializable> T deserializeObject(String serialized) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(serialized.getBytes(StandardCharsets.ISO_8859_1));
             ObjectInputStream ois = new ObjectInputStream(bais)) {

            Object object = ois.readObject();

            return (T) object;
        } catch (IOException | ClassNotFoundException | ClassCastException ex) {
            Log.e("Mixing", "Unable to construct object from store state", ex);
            return null;
        }
    }

    /**
     * Stores the mixing state of the client into SharedPreferences as ISO_8859_1 strings.
     * @param client Client to store the state for.
     */
    private static void storeMixingState(MixingClient client) {
        SharedPreferences settings = client.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        assert (client.cipher != null);
        assert (client.mixingToken != null);
        assert (client.mixingParameters != null);

        byte[] cipherSerialized = serializeObject(client.cipher);
        byte[] mixingTokenSerialized = serializeObject(client.mixingToken);
        byte[] mixingParametersSerialized = serializeObject(client.mixingParameters);

        if (cipherSerialized == null || mixingTokenSerialized == null || mixingParametersSerialized == null) {
            return;
        }

        SharedPreferences.Editor editor = settings.edit();

        editor.putString(MIXING_CIPHER_STORE, new String(cipherSerialized, StandardCharsets.ISO_8859_1));
        editor.putString(MIXING_TOKEN_STORE, new String(mixingTokenSerialized, StandardCharsets.ISO_8859_1));
        editor.putString(MIXING_PARAMETER_STORE, new String(mixingParametersSerialized, StandardCharsets.ISO_8859_1));

        editor.apply();
    }

    /**
     * Loads a previous mixing state if there's one in storage, else it does nothing.
     * @param client Client to load the state into.
     */
    private static void loadMixingState(MixingClient client) {
        SharedPreferences settings = client.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String cipherSerialized = settings.getString(MIXING_CIPHER_STORE, null);
        String mixingTokenSerialized = settings.getString(MIXING_TOKEN_STORE, null);
        String mixingParametersSerialized = settings.getString(MIXING_PARAMETER_STORE, null);

        if (cipherSerialized == null || mixingTokenSerialized == null || mixingParametersSerialized == null) {
            return;
        }

        client.cipher = deserializeObject(cipherSerialized);
        client.mixingToken = deserializeObject(mixingTokenSerialized);
        client.mixingParameters = deserializeObject(mixingParametersSerialized);
    }

    public static MixingClient getInstance(Context context) {
        if (instance == null) {
            Wallet passWallet = WalletUtil.fromKey(context, WalletRole.PASS);
            Wallet ballotWallet = WalletUtil.fromKey(context, WalletRole.BALLOT);
            instance = new MixingClient(context, passWallet, ballotWallet);
            loadMixingState(instance);
        }

        return instance;
    }
}
