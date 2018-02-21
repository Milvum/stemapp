package com.milvum.stemapp.geth.implementation.contract;

import android.content.Context;
import android.util.Log;

import com.milvum.stemapp.ballotexchange.TransferEventResponseWithLog;
import com.milvum.stemapp.contracts.VotingBallot;
import com.milvum.stemapp.geth.implementation.BlockchainConnection;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.Web3jProvider;
import com.milvum.stemapp.model.VoteCandidate;
import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.Utils;
import com.milvum.stemapp.utils.VoteItemUtils;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tx.VotingBallotWrapper;
import org.web3j.tx.WalletTransactionManager;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;


/**
 * .
 */

public class VotingBallotUtil {
    private static final String TAG = "VotingBallotUtil";

    public static VotingBallot getContract(Context context, Wallet wallet) {
        return getContractWrapper(context, wallet).getVotingBallot();
    }

    public static VotingBallotWrapper getContractWrapper(Context context, Wallet wallet) {
        Web3j web3j = Web3jProvider.getWeb3j();
        String contactAddress = Utils.getContractInfo(context).votingBallotAddressHex;
        WalletTransactionManager transactionManager = new WalletTransactionManager(web3j, wallet);

        return new VotingBallotWrapper(VotingBallot.load(
                contactAddress,
                web3j,
                transactionManager,
                BigInteger.valueOf(Constants.LOAD_GAS_PRICE),
                BigInteger.valueOf(Constants.LOAD_GAS_LIMIT)));
    }

    public static Observable<VotingBallot.GiveEventResponse> getBallotObservable(
            Context context,
            final Wallet ballotWallet,
            DefaultBlockParameter from,
            DefaultBlockParameter to) {
        VotingBallot ballotContract = getContract(context, ballotWallet);

        return ballotContract.giveEventObservable(from, to)
                .filter(event -> ballotWallet.getAddressHex().equals(event.to));
    }

    public static Observable<TransferEventResponseWithLog> onVote(
            Context context,
            Wallet wallet,
            DefaultBlockParameter from,
            DefaultBlockParameter to) {

        VotingBallotWrapper ballotWrapper = getContractWrapper(context, wallet);

        Log.v("Masking", "making a Transfer observable");

        return filterVotesToCandidates(context, ballotWrapper.transferEventLogObservable(from, to));
    }

    public static Observable<TransferEventResponseWithLog> replayVotes(
            Context context,
            Wallet wallet,
            DefaultBlockParameter from,
            DefaultBlockParameter to) throws IOException {

        VotingBallotWrapper ballotWrapper = getContractWrapper(context, wallet);
        Observable<TransferEventResponseWithLog> transferObservable =
                ballotWrapper.getTransferEventLogs(from, to);

        return filterVotesToCandidates(context, transferObservable);
    }

    private static Observable<TransferEventResponseWithLog> filterVotesToCandidates(
            Context context,
            Observable<TransferEventResponseWithLog> observable) {

        List<VoteCandidate> candidates = VoteItemUtils.getMainVoteItem(context).flatList();
        final Set<String> candidateAddresses = new HashSet<>();

        for (VoteCandidate candidate : candidates) {
            candidateAddresses.add("0x" + candidate.getAddress());
        }

        return observable.filter(event -> candidateAddresses.contains(event.to));
    }
}
