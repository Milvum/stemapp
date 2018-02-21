package com.milvum.stemapp.ballotexchange;

import com.milvum.stemapp.contracts.VotingBallot;

import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;

/**
 * .
 */

public class TransferEventResponseWithLog extends VotingBallot.TransferEventResponse {
    private final Log log;

    public TransferEventResponseWithLog(Log log) {
        this.log = log;
    }

    public TransferEventResponseWithLog(Log log, String from, String to, BigInteger value) {
        this(log);
        this.from = from;
        this.to = to;
        this.value = value;
    }

    public Log getLog() {
       return log;
    }
}
