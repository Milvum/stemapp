package com.milvum.stemapp.geth.implementation;

import com.milvum.stemapp.BuildConfig;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * .
 */

public class Web3jProvider {
    private static Web3j web3jInstance = null;

    public static Web3j getWeb3j() {
        if (web3jInstance == null) {
            web3jInstance = new JsonRpc2_0Web3j(new HttpService(BuildConfig.NODE_ADDRESS));
        }

        return web3jInstance;
    }
}
