package com.milvum.stemapp;

import com.milvum.stemapp.model.Cipher;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Random;

/**
 * .
 */

public class CipherTest {

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    public BigInteger encrypt(BigInteger m) {
        return m.modPow(privateKey.getPrivateExponent(), privateKey.getModulus());
    }

    public BigInteger decrypt(BigInteger m) {
        return m.modPow(publicKey.getPublicExponent(), publicKey.getModulus());
    }

    public BigInteger sign(BigInteger m) {
        return encrypt(m);
    }

    @Before
    public void setUp() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = keyGen.generateKeyPair();

        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        publicKey = (RSAPublicKey) keyPair.getPublic();
    }

    @Test
    public void sanityTest1() throws Exception {
        BigInteger value = BigInteger.valueOf(555L);
        BigInteger encrypted = encrypt(value);
        BigInteger decrypted = decrypt(encrypted);

        assertEquals(value, decrypted);
    }

    @Test
    public void sanityTest2() throws Exception {
        String payload = "blablaalksadjasoeidj";
        BigInteger value = Cipher.stringToBigInt(payload);

        BigInteger encrypted = value.modPow(privateKey.getPrivateExponent(), privateKey.getModulus());
        BigInteger decrypted = encrypted.modPow(publicKey.getPublicExponent(), publicKey.getModulus());

        String result = Cipher.bigIntToString(decrypted);

        assertEquals(payload, result);
    }

    @Test
    public void stringToIntToStringTest() throws Exception {
        String s = "PAISJDOIJOU)(@U#No0q8u0euq24enN)*U)@$QIUB*@!)E(NNX!@EMQSAX";

        assertEquals(s, Cipher.bigIntToString(Cipher.stringToBigInt(s)));
    }

    @Ignore("Does not work yet due to encoding issues")
    @Test
    public void IntToStringToInt() throws Exception {
        BigInteger b = new BigInteger(1024, new Random());

        assertEquals("Converting BigInt->string->BigInt should be same BigInt",
                     b, Cipher.stringToBigInt(Cipher.bigIntToString(b)));
    }

    @Test
    public void noBlindingFactor() throws Exception {
        String payload = "OIJASDOIJASD";
        BigInteger numericPayload = new BigInteger(payload.getBytes());
        BigInteger N = publicKey.getModulus();

        assertTrue("Message cannot be larger than N",
                numericPayload.compareTo(N) < 1);

        // blind
        Cipher cipher = new Cipher(BigInteger.ONE, publicKey.getPublicExponent(), publicKey.getModulus());
        BigInteger blinded = cipher.blind(numericPayload);

        assertEquals("Blinded message with no blinding factor equals original message",
                numericPayload, blinded);

        // Sign
        BigInteger signed = sign(blinded);

        assertEquals("Blinded (with b=1), signed message should be decryptable",
                numericPayload, decrypt(signed));
        assertTrue("Unblinded, signed message should equal message after decryption",
                cipher.verifyBlindedSignature(payload, signed));
    }

    @Test
    public void blindAndVerify() throws Exception {
        String payload = "blablabla";
        BigInteger numericPayload = new BigInteger(payload.getBytes());

        // Blind
        Cipher cipher = new Cipher(payload, publicKey.getPublicExponent(), publicKey.getModulus());
        BigInteger blinded = cipher.blind(numericPayload);

        // Sign
        BigInteger signed = sign(blinded);

        assertTrue("Unblinded, signed message should equal message after decryption",
                cipher.verifyBlindedSignature(payload, signed));
    }
}
