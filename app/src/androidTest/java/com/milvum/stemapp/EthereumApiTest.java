/**
 * .
 */
package com.milvum.stemapp;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.ethereum.geth.Account;
import org.ethereum.geth.Address;
import org.ethereum.geth.BigInt;
import org.ethereum.geth.EthereumClient;
import org.ethereum.geth.Geth;
import org.ethereum.geth.KeyStore;
import org.ethereum.geth.NewHeadHandler;
import org.ethereum.geth.Node;
import org.ethereum.geth.NodeConfig;
import org.ethereum.geth.NodeInfo;
import org.ethereum.geth.Transaction;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;

import static junit.framework.Assert.fail;


@RunWith(AndroidJUnit4.class)
public class EthereumApiTest {
    private static final String TAG = "EthereumApiTest";

    /* Edited code from https://github.com/ethereum/go-ethereum/wiki/Mobile-Clients:-Libraries-and-Inproc-Ethereum-Nodes */
    @Test
    public void testEthereumNode_connectMainChain() throws Exception {
        File filesDir = InstrumentationRegistry.getTargetContext().getFilesDir();
        org.ethereum.geth.Context gethContext = new org.ethereum.geth.Context();

        Node node = Geth.newNode(filesDir + "/.ethereum", new NodeConfig());
        node.start();

        NodeInfo info = node.getNodeInfo();
        android.util.Log.d(TAG, "My name: " + info.getName() + "\n");
        android.util.Log.d(TAG, "My address: " + info.getListenerAddress() + "\n");
        android.util.Log.d(TAG, "My protocols: " + info.getProtocols() + "\n\n");

        EthereumClient ec = node.getEthereumClient();
        android.util.Log.d(TAG, "Latest block: " + ec.getBlockByNumber(gethContext, -1).getNumber() + ", syncing...\n");

        // Unfortunately, the test does not wait for onNewHead to be called
        NewHeadHandler handler = new NewHeadHandler() {
            @Override public void onError(String error) { }
            @Override public void onNewHead(final org.ethereum.geth.Header header) {
                android.util.Log.d(TAG, "#" + header.getNumber() + ": " + header.getHash().getHex().substring(0, 10) + "…\n");
            }
        };

        ec.subscribeNewHead(gethContext, handler,  16);
    }

    /* Edited code from https://github.com/ethereum/go-ethereum/blob/master/mobile/android_test.go */
    @Test
    public void testKeystore() throws Exception {
        File filesDir = InstrumentationRegistry.getTargetContext().getFilesDir();

        // Create an encrypted keystore with light crypto parameters.
        KeyStore ks = new KeyStore(filesDir + "/keystore", Geth.LightScryptN, Geth.LightScryptP);

        // Create a new account with the specified encryption passphrase.
        Account newAcc = ks.newAccount("Creation password");

        // Export the newly created account with a different passphrase. The returned
        // data from this method invocation is a JSON encoded, encrypted key-file.
        byte[] jsonAcc = ks.exportKey(newAcc, "Creation password", "Export password");

        // Update the passphrase on the account created above inside the local keystore.
        ks.updateAccount(newAcc, "Creation password", "Update password");

        // Delete the account updated above from the local keystore.
        ks.deleteAccount(newAcc, "Update password");

        // Import back the account we've exported (and then deleted) above with yet
        // again a fresh passphrase.
        Account impAcc = ks.importKey(jsonAcc, "Export password", "Import password");

        // Create a new account to sign transactions with
        Account signer = ks.newAccount("Signer password");

        Transaction tx = new Transaction(
                1, new Address("0x0000000000000000000000000000000000000000"),
                new BigInt(0), new BigInt(0), new BigInt(1), null); // Random empty transaction
        BigInt chain = new BigInt(1); // Chain identifier of the main net

        // Sign a transaction with a single authorization
        Transaction signed = ks.signTxPassphrase(signer, "Signer password", tx, chain);

        // Sign a transaction with multiple manually cancelled authorizations
        ks.unlock(signer, "Signer password");
        signed = ks.signTx(signer, tx, chain);
        ks.lock(signer.getAddress());

        // Sign a transaction with multiple automatically cancelled authorizations
        ks.timedUnlock(signer, "Signer password", 1000000000);
        signed = ks.signTx(signer, tx, chain);
    }

    /* From https://github.com/ethereum/go-ethereum/blob/master/mobile/android_test.go */
    // Tests that recovering transaction signers works for both Homestead and EIP155
    // signatures too. Regression test for go-ethereum issue #14599.
    @Test
    public void testIssue14599() {
        try {
            byte[] preEIP155RLP = new BigInteger("f901fc8032830138808080b901ae60056013565b6101918061001d6000396000f35b3360008190555056006001600060e060020a6000350480630a874df61461003a57806341c0e1b514610058578063a02b161e14610066578063dbbdf0831461007757005b610045600435610149565b80600160a060020a031660005260206000f35b610060610161565b60006000f35b6100716004356100d4565b60006000f35b61008560043560243561008b565b60006000f35b600054600160a060020a031632600160a060020a031614156100ac576100b1565b6100d0565b8060018360005260205260406000208190555081600060005260206000a15b5050565b600054600160a060020a031633600160a060020a031614158015610118575033600160a060020a0316600182600052602052604060002054600160a060020a031614155b61012157610126565b610146565b600060018260005260205260406000208190555080600060005260206000a15b50565b60006001826000526020526040600020549050919050565b600054600160a060020a031633600160a060020a0316146101815761018f565b600054600160a060020a0316ff5b561ca0c5689ed1ad124753d54576dfb4b571465a41900a1dff4058d8adf16f752013d0a01221cbd70ec28c94a3b55ec771bcbc70778d6ee0b51ca7ea9514594c861b1884", 16).toByteArray();
            preEIP155RLP = Arrays.copyOfRange(preEIP155RLP, 1, preEIP155RLP.length);

            byte[] postEIP155RLP = new BigInteger("f86b80847735940082520894ef5bbb9bba2e1ca69ef81b23a8727d889f3ef0a1880de0b6b3a7640000802ba06fef16c44726a102e6d55a651740636ef8aec6df3ebf009e7b0c1f29e4ac114aa057e7fbc69760b522a78bb568cfc37a58bfdcf6ea86cb8f9b550263f58074b9cc", 16).toByteArray();
            postEIP155RLP = Arrays.copyOfRange(postEIP155RLP, 1, postEIP155RLP.length);

            Transaction preEIP155 = new Transaction(preEIP155RLP);
            Transaction postEIP155 = new Transaction(postEIP155RLP);

            preEIP155.getFrom(null);           // Homestead should accept homestead
            preEIP155.getFrom(new BigInt(4));  // EIP155 should accept homestead (missing chain ID)
            postEIP155.getFrom(new BigInt(4)); // EIP155 should accept EIP 155

            try {
                postEIP155.getFrom(null);
                fail("EIP155 transaction accepted by Homestead");
            } catch (Exception e) {
            }
        } catch (Exception e) {
            fail(e.toString());
        }
    }
}
