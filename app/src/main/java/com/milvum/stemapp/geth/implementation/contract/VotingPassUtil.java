package com.milvum.stemapp.geth.implementation.contract;

import android.content.Context;

import com.milvum.stemapp.contracts.VotingBallot;
import com.milvum.stemapp.contracts.VotingPass;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.Web3jProvider;
import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.Utils;

import org.web3j.protocol.Web3j;
import org.web3j.tx.WalletTransactionManager;

import java.math.BigInteger;

/**
 * .
 */

public class VotingPassUtil {
    private static VotingPass contract = null;

    public static VotingPass getContract(Context context, Wallet wallet) {
        if(contract != null) {
            return contract;
        }

        if(wallet == null) {
            throw new AssertionError("Wallet cannot be null if contract is not instantiated");
        }

        Web3j web3j = Web3jProvider.getWeb3j();
        String contactAddress = Utils.getContractInfo(context).votingPassAddressHex;
        WalletTransactionManager transactionManager = new WalletTransactionManager(web3j, wallet);

        contract = VotingPass.load(
                contactAddress,
                web3j,
                transactionManager,
                BigInteger.valueOf(Constants.LOAD_GAS_PRICE),
                BigInteger.valueOf(Constants.LOAD_GAS_LIMIT));

        return contract;
    }

}
