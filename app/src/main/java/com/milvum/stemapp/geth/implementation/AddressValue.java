package com.milvum.stemapp.geth.implementation;

/**
 * .
 */

public class AddressValue extends HexValue {
    public static String NULL_ADDRESS = "0x0000000000000000000000000000000000000000";

    public AddressValue(String hexString) {
        super(hexString);
    }

    public AddressValue(byte[] hexBytes) {
        super(hexBytes);
    }

    public AddressValue(org.ethereum.geth.Address gethAddress) {
        super(gethAddress.getHex());
    }

    public AddressValue(org.web3j.abi.datatypes.Address web3jAddress) {
        super(web3jAddress.toString());
    }

    public org.ethereum.geth.Address toGethAddress() {
        if (toString().equals("0x00")) {
            return new org.ethereum.geth.Address(NULL_ADDRESS);
        }

        return new org.ethereum.geth.Address(toString());
    }

    public org.web3j.abi.datatypes.Address toWeb3jAddress() {
        return new org.web3j.abi.datatypes.Address(toString());
    }
}
