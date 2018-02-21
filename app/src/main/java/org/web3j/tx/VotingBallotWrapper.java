package org.web3j.tx;

import com.milvum.stemapp.ballotexchange.TransferEventResponseWithLog;
import com.milvum.stemapp.contracts.VotingBallot;
import com.milvum.stemapp.geth.implementation.Web3jProvider;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

/**
 * .
 *
 * A wrapper for the generated VotingBallot class that adds extra functionality.
 * It is located in the org.web3j.tx package to access protected members of org.web3j.tx.Contract.
 */

public class VotingBallotWrapper {
    private VotingBallot votingBallot;

    private Event transferEvent;

    public VotingBallotWrapper(VotingBallot votingBallot) {
       this.votingBallot = votingBallot;
       transferEvent = new Event("Transfer",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    }

    public VotingBallot getVotingBallot() {
        return votingBallot;
    }

    /**
     * Similar to VotingBallot.transferEventObservable, except that the corresponding Log object
     *   is included with each event.
     */
    public Observable<TransferEventResponseWithLog> transferEventLogObservable(
                    DefaultBlockParameter startBlock,
                    DefaultBlockParameter endBlock) {
        Web3j web3j = Web3jProvider.getWeb3j();

        EthFilter filter = getTransferEventFilter(startBlock, endBlock);

        return mapToTransferEventResponseWithLog(web3j.ethLogObservable(filter));
    }

    public Observable<TransferEventResponseWithLog> getTransferEventLogs(
            DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {

        Web3j web3j = Web3jProvider.getWeb3j();

        List<EthLog.LogResult> logs = null;
        try {
            logs = web3j.ethGetLogs(getTransferEventFilter(startBlock, endBlock))
                    .send()
                    .getLogs();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mapToTransferEventResponseWithLog(mapToLog(Observable.from(logs)));
    }


    private Observable<Log> mapToLog(Observable<EthLog.LogResult> logResultObservable) {
        return logResultObservable.map(new Func1<EthLog.LogResult, Log>() {
            @Override
            public Log call(EthLog.LogResult logResult) {
                return ((EthLog.LogObject) logResult).get();
            }
        });
    }

    private Observable<TransferEventResponseWithLog>
            mapToTransferEventResponseWithLog(Observable<Log> logObservable) {
        return logObservable.map(new Func1<Log, TransferEventResponseWithLog>() {
            @Override
            public TransferEventResponseWithLog call(Log log) {
                EventValues eventValues = votingBallot.extractEventParameters(getTransferEvent(), log);
                String from = (String) eventValues.getIndexedValues().get(0).getValue();
                String to = (String) eventValues.getIndexedValues().get(1).getValue();
                BigInteger value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();

                return new TransferEventResponseWithLog(log, from, to, value);
            }
        });
    }

    public EthFilter getTransferEventFilter(
            DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, votingBallot.getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(getTransferEvent()));

        return filter;
    }

    public Event getTransferEvent() {
        return this.transferEvent;
    }
}
