package com.milvum.stemapp.model;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * .
 */

public class MixingToken implements Serializable {
    // Update this when you change the object signature (i.e. add/remove/modify fields)
    private static final long serialVersionUID = 1L;
    private String addressHex;
    private BigInteger nonce;

    public MixingToken(String addressHex, BigInteger nonce) {
        this.addressHex = addressHex;
        this.nonce = nonce;
    }

    @Override
    public String toString() {
        return addressHex + "-" + nonce.toString();
    }
}
