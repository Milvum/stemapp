package com.milvum.stemapp.geth.implementation;

import org.spongycastle.util.encoders.Hex;

/**
 * .
 *
 * Represents a hexidecimal value,
 * Use this class if you don't want to worry about whether it is stored as as String or a byte[].
 */

public class HexValue {
    final byte[] hexBytes;
    final String hexString;

    public HexValue(String hexString) {
        if (hexString == null) {
            this.hexString = "0x00";
        } else {
            this.hexString = hexString;
        }

        assert(this.hexString.substring(2).equals("0x"));

        this.hexBytes = Hex.decode(this.hexString.substring(2));
    }

    public HexValue(byte[] hexBytes) {
        if (hexBytes == null) {
            this.hexBytes = new byte[] { 0 };
        } else {
            this.hexBytes = hexBytes;
        }

        this.hexString = "0x" + Hex.toHexString(this.hexBytes);
    }

    @Override
    public String toString() {
        return hexString;
    }

    public byte[] toBytes() {
        return hexBytes;
    }
}
