package com.milvum.stemapp.geth.implementation;

import android.content.Context;

import com.milvum.stemapp.contracts.VotingBallot;
import com.milvum.stemapp.model.VoteCandidate;
import com.milvum.stemapp.utils.Utils;

import org.web3j.abi.datatypes.Address;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.WalletTransactionManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * .
 */

public class VoteCaster {
    private static final String TAG = "VoteCaster";

    private static final long LOAD_GAS_LIMIT = 2000000L;
    private static final long LOAD_GAS_PRICE = 40000000000L;

    private String ballotContractAddressHex;
    private Web3j web3j;

    private VotingBallot ballotContract;

    public VoteCaster(Context context) {
        this.ballotContractAddressHex = Utils.getContractInfo(context).votingBallotAddressHex;
        this.web3j = Web3jProvider.getWeb3j();
    }

    private void loadContracts(Wallet wallet) {
        TransactionManager manager = new WalletTransactionManager(web3j, wallet);
        ballotContract = VotingBallot.load(
                ballotContractAddressHex,
                web3j,
                manager,
                BigInteger.valueOf(LOAD_GAS_PRICE),
                BigInteger.valueOf(LOAD_GAS_LIMIT));
    }

    // pre: wallet must have a voting ballot
    public Future<TransactionReceipt> run(Wallet wallet, VoteCandidate candidate) {
        Address candidateAddress = new Address(candidate.getAddress());

        loadContracts(wallet);

        return ballotContract.transfer(candidateAddress.toString(), BigInteger.ONE).sendAsync();
    }

    /**
     * pre: wallet must have candidates.size() voting ballots
     * Casts a vote for every given VoteCandidate
     * BLOCKS until each vote has completed (either successfully or with an error)
     */
    public List<Future<TransactionReceipt>> run(Wallet wallet, List<VoteCandidate> candidates)
            throws InterruptedException {
        loadContracts(wallet);

        // Collect the vote callables in a list
        List<Callable<TransactionReceipt>> callables = new ArrayList<>();
        for (VoteCandidate candidate : candidates) {
            callables.add(() ->
                    ballotContract.transfer(
                            new Address(candidate.getAddress()).toString(),
                            BigInteger.ONE).send());
        }

        // @TODO use the number of available cores
        ExecutorService executor = Executors.newFixedThreadPool(2);
        // Blocks until all callables are completed (either successfully or with an error)
        List<Future<TransactionReceipt>> futures = executor.invokeAll(callables);
        return futures;
    }
}
