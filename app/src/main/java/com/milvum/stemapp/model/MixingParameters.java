package com.milvum.stemapp.model;

import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Type;
import org.web3j.tuples.generated.Tuple8;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Future;

/**
 * .
 */

public class MixingParameters implements Serializable {
    // Update this when you change the object signature (i.e. add/remove/modify fields)
    private static final long serialVersionUID = 1L;
    public final BigInteger deposit;
    public final BigInteger deadlineTransferClient;
    public final BigInteger deadlineProvideWarranty;
    public final BigInteger deadlineUnblindAddress;
    public final BigInteger deadlineTransferMixer;
    public final BigInteger minimumBlockAmount;
    public final BigInteger numberOfParticipants;
    public final Boolean isValid;

    public MixingParameters(Tuple8<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, Boolean> parameters) {
        this.deposit = parameters.getValue1();
        this.deadlineTransferClient = parameters.getValue2();
        this.deadlineProvideWarranty = parameters.getValue3();
        this.deadlineUnblindAddress = parameters.getValue4();
        this.deadlineTransferMixer = parameters.getValue5();
        this.minimumBlockAmount = parameters.getValue6();
        this.numberOfParticipants = parameters.getValue7();
        this.isValid = parameters.getValue8();
    }
}
