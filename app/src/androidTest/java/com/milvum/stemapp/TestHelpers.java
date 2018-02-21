package com.milvum.stemapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.widget.Toast;

import com.milvum.stemapp.contracts.VotingBallot;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.Web3jProvider;
import com.milvum.stemapp.geth.implementation.contract.VotingBallotUtil;
import com.milvum.stemapp.geth.implementation.contract.VotingPassUtil;
import com.milvum.stemapp.model.WalletRole;
import com.milvum.stemapp.utils.ToastUtil;
import com.milvum.stemapp.utils.VoteUtil;
import com.milvum.stemapp.utils.WalletUtil;

import org.web3j.protocol.core.DefaultBlockParameterName;

import java.io.File;
import java.math.BigInteger;

/**
 * .
 */

public class TestHelpers
{
    public static void clearState()
    {
        File root = InstrumentationRegistry.getTargetContext().getFilesDir().getParentFile();
        String[] sharedPreferencesFileNames = new File(root, "shared_prefs").list();

        if (sharedPreferencesFileNames != null)
        {
            for (String fileName : sharedPreferencesFileNames)
            {
                InstrumentationRegistry.getTargetContext().getSharedPreferences(fileName.replace(".xml", ""), Context.MODE_PRIVATE).edit().clear().commit();
            }
        }
    }

    public static boolean isAbleToVote(Context context)
    {
        Wallet passWallet = WalletUtil.fromKey(context, WalletRole.PASS);
        Wallet ballotWallet = WalletUtil.fromKey(context, WalletRole.BALLOT);

        BigInteger passBalance, ballotBalance, etherBalance;
        try
        {
            passBalance = VotingPassUtil.getContract(context, passWallet)
                    .balanceOf(passWallet.getAddressHex()).send();
            ballotBalance = VotingBallotUtil.getContract(context, ballotWallet)
                    .balanceOf(ballotWallet.getAddressHex()).send();
            etherBalance = Web3jProvider.getWeb3j().ethGetBalance(
                    passWallet.getAddressHex(),
                    DefaultBlockParameterName.LATEST).send().getBalance();
        } catch (Exception e)
        {
            return false;
        }

        if (ballotBalance.compareTo(BigInteger.ZERO) > 0)
        {
            return true;
        }

        // Check Ether balance
        if (etherBalance.compareTo(BigInteger.ZERO) != 1)
        {

            return false;
        }

        // Check Pass balance
        if (passBalance.compareTo(BigInteger.ZERO) != 1)
        {
            return false;
        }
        return true;
    }

    public static boolean hasReceivedBallot(Context context) throws Exception
    {
        Wallet ballotWallet = WalletUtil.fromKey(context, WalletRole.BALLOT);
        VotingBallot ballotContract = VotingBallotUtil.getContract(context, ballotWallet);

        BigInteger ballotBalance = ballotContract.balanceOf(ballotWallet.getAddressHex()).send();
        return ballotBalance.compareTo(BigInteger.ZERO) == 1;
    }

    public static boolean isAbleToVerifyVote(Context context) throws InterruptedException
    {
        return VoteUtil.getInstance(context).getIdSet().size() <= 1;
    }
}
