package com.milvum.stemapp.model;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * .
 * Helper class for blinding and unblinding Strings using the properties of RSA Blind Signing.
 * This class only provides the means for a client, the actual signing is performed by another party.
 */

public class Cipher implements Serializable {
    // Update this when you change the object signature (i.e. add/remove/modify fields)
    private static final long serialVersionUID = 1L;
    private BigInteger blindingFactor;
    private BigInteger publicExponent;
    private BigInteger modulus;

    public Cipher(String payload, BigInteger publicExponent, BigInteger modulus) {
        this(generateBlindingFactor(payload), publicExponent, modulus);
    }

    public Cipher(BigInteger blindingFactor, BigInteger publicExponent, BigInteger modulus) {
        this.publicExponent = publicExponent;
        this.modulus = modulus;
        this.blindingFactor = blindingFactor;
    }

    /**
     * Blinds the input using the publicKey and blindingfactor. This allows another
     * party to sign with the secret counterpart of publicKey while being unable to read the payload.
     * @param m - The input that will be blinded
     * @return - The blinded payload
     */
    public BigInteger blind(BigInteger m) {

        // _m_ := m * b^key mod N
        return this.blindingFactor.modPow(this.publicExponent, this.modulus)
                .multiply(m).mod(this.modulus);
    }

    /**
     * Unblinds the input with the publicKey and blindingfactor. If payload was signed with secret
     * counterpart of publicKey, the result will be an unblinded, signed payload.
     * @param sigma - The input that will be unblinded
     * @return - The unblinded payload
     */
    public BigInteger unblind(BigInteger sigma) {
        BigInteger bInverse = blindingFactor.modInverse(this.modulus);

        // sig := _sig_ * b^-1 mod N
        return sigma.multiply(bInverse).mod(this.modulus);
    }

    /**
     * Verify whether given signature (that is still blinded) is that of the original message
     * @param original - The original message.
     * @param blindedSignature - The to be tested signature (performed on the blinded message)
     * @return - A boolean of whether blindedSignature is a proper signature of original
     */
    public boolean verifyBlindedSignature(String original, BigInteger blindedSignature) {
        BigInteger signature = unblind(blindedSignature);

        return verifySignature(original, signature);
    }

    /**
     * Verify whether given signature is one of the orignal message
     * @param original - The original message
     * @param signature - The to be tested signature
     * @return - A boolean of whether signature
     */
    public boolean verifySignature(String original, BigInteger signature) {
        BigInteger message = Cipher.stringToBigInt(original);
        BigInteger message2 = signature.modPow(publicExponent, modulus);

        // message == signature^pubkey mod N
        return message.equals(message2);
    }

    /**
     * Generate a blinding factor of a size order similar to payload
     * @param payload - The data that will eventually be blinded with this blinding factor
     * @return - The blinding factor
     */
    public static BigInteger generateBlindingFactor(String payload) {
        byte[] blindingFactor = new byte[payload.getBytes().length];

        new Random().nextBytes(blindingFactor);

        return new BigInteger(blindingFactor);
    }

    /**
     * Convert a string to a BigInteger such that it can be used to perform calculations with
     * (like encryption).
     * TODO: This might need to be moved somewhere else.
     * @param s - The string to be converted
     * @return - The resulting BigInteger
     */
    public static BigInteger stringToBigInt(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        return new BigInteger(bytes);
    }

    /**
     * Convert a BigInteger to a String. The BigInteger numeric value of the bytes used to compose
     * the String. This is not a conversion of 123 to "123"!
     * TODO: This might need to be moved somewhere else
     * @param i - The BigInteger to be converted
     * @return - The resulting String
     */
    public static String bigIntToString(BigInteger i) {
        byte[] bytes = i.toByteArray();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
