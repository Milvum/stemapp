package com.milvum.stemapp.geth.implementation;

import android.content.Context;

import com.fasterxml.jackson.databind.ser.std.StdKeySerializers;
import com.milvum.stemapp.contracts.BallotDispenser;
import com.milvum.stemapp.utils.Utils;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * .
 */

public class MixingObserver {
    private static final String TAG = "MixingObserver";

    private final HexValue ballotDispenserAddress;
    private Web3j web3j;

    public MixingObserver(Context context) {
        this(new AddressValue(Utils.getContractInfo(context).ballotDispenserAddressHex));
    }

    /**
     * Creates a MixingObserver that can be used to subscribe to events emitted
     *   by the BallotDispeenser contract at the given ballotDispenserAddress.
     * @param ballotDispenserAddress
     */
    public MixingObserver(AddressValue ballotDispenserAddress) {
        this.ballotDispenserAddress = ballotDispenserAddress;
        this.web3j = Web3jProvider.getWeb3j();
    }

    // Function taken from org.web3j.tx.Contract
    private EventValues extractEventParameters(
            Event event, Log log) {

        List<String> topics = log.getTopics();
        String encodedEventSignature = EventEncoder.encode(event);
        if (!topics.get(0).equals(encodedEventSignature)) {
            return null;
        }

        List<Type> indexedValues = new ArrayList<Type>();
        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(
                log.getData(), event.getNonIndexedParameters());

        List<TypeReference<Type>> indexedParameters = event.getIndexedParameters();
        for (int i = 0; i < indexedParameters.size(); i++) {
            Type value = FunctionReturnDecoder.decodeIndexedValue(
                    topics.get(i + 1), indexedParameters.get(i));
            indexedValues.add(value);
        }
        return new EventValues(indexedValues, nonIndexedValues);
    }

    // Encode a HexValue as a fixed size hexadecimal value.
    // Some Ethereum/Solidity types (such as address) are encoded as such a value.
    // Inspired by TypeEncoder in org.web3j.abi, although we convert to a slightly different output here
    private String encodeAsFixedSizeHex(HexValue value) {
        byte[] byteValue = value.toBytes();
        int length = byteValue.length;
        int mod = length % Type.MAX_BYTE_LENGTH;

        byte[] dest;
        if (mod != 0) {
            // Pad the byte[] with zeroes at the start
            int padding = Type.MAX_BYTE_LENGTH - mod;
            dest = new byte[length + padding];
            System.arraycopy(byteValue, 0, dest, padding, length);
        } else {
            dest = byteValue;
        }

        // Convert to "0x"-padded hex string
        return new HexValue(dest).toString();
    }


    // Returns an observable that listens to JoinRequested events concerning the given filterAddress
    // Looks for events in ALL blocks (Earliest - Latest)
    public Observable<BallotDispenser.JoinRequestedEventResponse> getJoinRequestedObservable(Context context, AddressValue filterAddress) {
        return getJoinRequestedObservable(filterAddress, DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST);
    }

    // Returns an observable that listens to JoinRequested events concerning the given filterAddress.
    // Looks for events in the given block range
    public Observable<BallotDispenser.JoinRequestedEventResponse> getJoinRequestedObservable(
            AddressValue filterAddress, DefaultBlockParameter fromBlock, DefaultBlockParameter toBlock) {

        final Event event = new Event("JoinRequested",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}),
                Arrays.<TypeReference<?>>asList(
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Utf8String>() {}));

        // Compose the filter
        EthFilter filter = new EthFilter(
                fromBlock,
                toBlock,
                ballotDispenserAddress.toString());
        filter.addSingleTopic(EventEncoder.encode(event));

        String encodedAddress = encodeAsFixedSizeHex(filterAddress);
        filter.addSingleTopic(encodedAddress);


        return web3j.ethLogObservable(filter).map(new Func1<Log, BallotDispenser.JoinRequestedEventResponse>() {
            @Override
            public BallotDispenser.JoinRequestedEventResponse call(Log log) {
                android.util.Log.d(TAG, "Got JoinRequested event with topics: \n" + log.getTopics());

                // Re-package the log entries into nice event objects
                EventValues eventValues = extractEventParameters(event, log);
                List<Type> nonIndexed = eventValues.getNonIndexedValues();
                BallotDispenser.JoinRequestedEventResponse typedResponse = new BallotDispenser.JoinRequestedEventResponse();
                typedResponse.client = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.deposit = (BigInteger) nonIndexed.get(0).getValue();
                typedResponse.deadlineTransferClient = (BigInteger) nonIndexed.get(1).getValue();
                typedResponse.deadlineProvideWarranty = (BigInteger) nonIndexed.get(2).getValue();
                typedResponse.deadlineUnblindAddress = (BigInteger) nonIndexed.get(3).getValue();
                typedResponse.deadlineTransferMixer = (BigInteger) nonIndexed.get(4).getValue();
                typedResponse.minimumBlockAmount = (BigInteger) nonIndexed.get(5).getValue();
                typedResponse.mixToken = (String) nonIndexed.get(6).getValue();

                return typedResponse;
            }
        });
    }

    // Returns an observable that listens to JoinAccepted events concerning the given filterAddress
    // Looks for events in ALL blocks (Earliest - Latest)
    public Observable<BallotDispenser.JoinAcceptedEventResponse> getJoinAcceptedObservable(AddressValue filterAddress) {
        return getJoinAcceptedObservable(filterAddress, DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST);
    }

    // Returns an observable that listens to JoinAccepted events concerning the given filterAddress.
    // Looks for events in the given block range
    public Observable<BallotDispenser.JoinAcceptedEventResponse> getJoinAcceptedObservable(
            AddressValue filterAddress, DefaultBlockParameter fromBlock, DefaultBlockParameter toBlock) {

        final Event event = new Event("JoinAccepted",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));

        // Compose the filter
        EthFilter filter = new EthFilter(
                fromBlock,
                toBlock,
                ballotDispenserAddress.toString());
        filter.addSingleTopic(EventEncoder.encode(event));

        String encodedAddress = encodeAsFixedSizeHex(filterAddress);
        filter.addSingleTopic(encodedAddress);


        return web3j.ethLogObservable(filter).map(new Func1<Log, BallotDispenser.JoinAcceptedEventResponse>() {
            @Override
            public BallotDispenser.JoinAcceptedEventResponse call(Log log) {
                android.util.Log.d(TAG, "Got JoinAccepted event with topics: \n" + log.getTopics());

                // Re-package the log entries into nice event objects
                EventValues eventValues = extractEventParameters(event, log);
                BallotDispenser.JoinAcceptedEventResponse typedResponse = new BallotDispenser.JoinAcceptedEventResponse();
                typedResponse.client = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.partialWarranty = (String) eventValues.getNonIndexedValues().get(0).getValue();

                return typedResponse;
            }
        });
    }


    // Returns an observable that listens to JoinRejected events concerning the given filterAddress
    // Looks for events in ALL blocks (Earliest - Latest)
    public Observable<BallotDispenser.JoinRejectedEventResponse> getJoinRejectedObservable(AddressValue filterAddress) {
        return getJoinRejectedObservable(filterAddress, DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST);
    }

    // Returns an observable that listens to JoinRejected events concerning the given filterAddress.
    // Looks for events in the given block range
    public Observable<BallotDispenser.JoinRejectedEventResponse> getJoinRejectedObservable(
            AddressValue filterAddress, DefaultBlockParameter fromBlock, DefaultBlockParameter toBlock) {

        final Event event = new Event("JoinRejected",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}),
                Arrays.<TypeReference<?>>asList());

        // Compose the filter
        EthFilter filter = new EthFilter(
                fromBlock,
                toBlock,
                ballotDispenserAddress.toString());
        filter.addSingleTopic(EventEncoder.encode(event));

        String encodedAddress = encodeAsFixedSizeHex(filterAddress);
        filter.addSingleTopic(encodedAddress);


        return web3j.ethLogObservable(filter).map(new Func1<Log, BallotDispenser.JoinRejectedEventResponse>() {
            @Override
            public BallotDispenser.JoinRejectedEventResponse call(Log log) {
                android.util.Log.d(TAG, "Got JoinRejected event with topics: \n" + log.getTopics());

                // Re-package the log entries into nice event objects
                EventValues eventValues = extractEventParameters(event, log);
                BallotDispenser.JoinRejectedEventResponse typedResponse = new BallotDispenser.JoinRejectedEventResponse();
                typedResponse.client = (String) eventValues.getIndexedValues().get(0).getValue();

                return typedResponse;
            }
        });
    }

    // Returns an observable that listens to WarrantyProvided events concerning the given filterAddress
    // Looks for events in ALL blocks (Earliest - Latest)
    public Observable<BallotDispenser.WarrantyProvidedEventResponse> getWarrantyProvidedObservable(AddressValue filterAddress) {
        return getWarrantyProvidedObservable(filterAddress, DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST);
    }

    // Returns an observable that listens to WarrantyProvided events concerning the given filterAddress.
    // Looks for events in the given block range
    public Observable<BallotDispenser.WarrantyProvidedEventResponse> getWarrantyProvidedObservable(
            AddressValue filterAddress, DefaultBlockParameter fromBlock, DefaultBlockParameter toBlock) {

        final Event event = new Event("WarrantyProvided",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));

        // Compose the filter
        EthFilter filter = new EthFilter(
                fromBlock,
                toBlock,
                ballotDispenserAddress.toString());
        filter.addSingleTopic(EventEncoder.encode(event));

        String encodedAddress = encodeAsFixedSizeHex(filterAddress);
        filter.addSingleTopic(encodedAddress);

        return web3j.ethLogObservable(filter).map(new Func1<Log, BallotDispenser.WarrantyProvidedEventResponse>() {
            @Override
            public BallotDispenser.WarrantyProvidedEventResponse call(Log log) {
                android.util.Log.d(TAG, "Got WarrantyProvided event with topics: \n" + log.getTopics());

                EventValues eventValues = extractEventParameters(event, log);
                BallotDispenser.WarrantyProvidedEventResponse typedResponse = new BallotDispenser.WarrantyProvidedEventResponse();
                typedResponse.client = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.warranty = (String) eventValues.getNonIndexedValues().get(0).getValue();

                return typedResponse;
            }
        });
    }

    private EthFilter composeFilter(Event event, DefaultBlockParameter fromBlock, DefaultBlockParameter toBlock) {
        EthFilter filter = new EthFilter(
                fromBlock,
                toBlock,
                ballotDispenserAddress.toString());
        filter.addSingleTopic(EventEncoder.encode(event));

//        String encodedAddress = encodeAsFixedSizeHex("");
  //      filter.addSingleTopic(encodedAddress);
        return null;
    }
}

