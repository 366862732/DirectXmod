/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.primitives.Longs
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  it.unimi.dsi.fastutil.bytes.ByteArrays
 */
package net.minecraft.util;

import com.google.common.primitives.Longs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.CryptException;

public class Crypt {
    private static final String SYMMETRIC_ALGORITHM = "AES";
    private static final int SYMMETRIC_BITS = 128;
    private static final String ASYMMETRIC_ALGORITHM = "RSA";
    private static final int ASYMMETRIC_BITS = 1024;
    private static final String BYTE_ENCODING = "ISO_8859_1";
    private static final String HASH_ALGORITHM = "SHA-1";
    public static final String SIGNING_ALGORITHM = "SHA256withRSA";
    public static final int SIGNATURE_BYTES = 256;
    private static final String PEM_RSA_PRIVATE_KEY_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PEM_RSA_PRIVATE_KEY_FOOTER = "-----END RSA PRIVATE KEY-----";
    public static final String RSA_PUBLIC_KEY_HEADER = "-----BEGIN RSA PUBLIC KEY-----";
    private static final String RSA_PUBLIC_KEY_FOOTER = "-----END RSA PUBLIC KEY-----";
    public static final String MIME_LINE_SEPARATOR = "\n";
    public static final Base64.Encoder MIME_ENCODER = Base64.getMimeEncoder(76, "\n".getBytes(StandardCharsets.UTF_8));
    public static final Codec<PublicKey> PUBLIC_KEY_CODEC = Codec.STRING.comapFlatMap(rsaString -> {
        try {
            return DataResult.success((Object)Crypt.stringToRsaPublicKey(rsaString));
        }
        catch (CryptException e) {
            return DataResult.error(e::getMessage);
        }
    }, Crypt::rsaPublicKeyToString);
    public static final Codec<PrivateKey> PRIVATE_KEY_CODEC = Codec.STRING.comapFlatMap(rsaString -> {
        try {
            return DataResult.success((Object)Crypt.stringToPemRsaPrivateKey(rsaString));
        }
        catch (CryptException e) {
            return DataResult.error(e::getMessage);
        }
    }, Crypt::pemRsaPrivateKeyToString);

    public static SecretKey generateSecretKey() throws CryptException {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
            keyGenerator.init(128);
            return keyGenerator.generateKey();
        }
        catch (Exception e) {
            throw new CryptException(e);
        }
    }

    public static KeyPair generateKeyPair() throws CryptException {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ASYMMETRIC_ALGORITHM);
            generator.initialize(1024);
            return generator.generateKeyPair();
        }
        catch (Exception e) {
            throw new CryptException(e);
        }
    }

    public static byte[] digestData(String serverId, PublicKey publicKey, SecretKey sharedKey) throws CryptException {
        try {
            return Crypt.digestData(serverId.getBytes(BYTE_ENCODING), sharedKey.getEncoded(), publicKey.getEncoded());
        }
        catch (Exception e) {
            throw new CryptException(e);
        }
    }

    private static byte[] digestData(byte[] ... inputs) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
        for (byte[] input : inputs) {
            messageDigest.update(input);
        }
        return messageDigest.digest();
    }

    private static <T extends Key> T rsaStringToKey(String input, String header, String footer, ByteArrayToKeyFunction<T> byteArrayToKey) throws CryptException {
        int begin = input.indexOf(header);
        if (begin != -1) {
            int end = input.indexOf(footer, begin += header.length());
            input = input.substring(begin, end + 1);
        }
        try {
            return byteArrayToKey.apply(Base64.getMimeDecoder().decode(input));
        }
        catch (IllegalArgumentException e) {
            throw new CryptException(e);
        }
    }

    public static PrivateKey stringToPemRsaPrivateKey(String rsaString) throws CryptException {
        return Crypt.rsaStringToKey(rsaString, PEM_RSA_PRIVATE_KEY_HEADER, PEM_RSA_PRIVATE_KEY_FOOTER, Crypt::byteToPrivateKey);
    }

    public static PublicKey stringToRsaPublicKey(String rsaString) throws CryptException {
        return Crypt.rsaStringToKey(rsaString, RSA_PUBLIC_KEY_HEADER, RSA_PUBLIC_KEY_FOOTER, Crypt::byteToPublicKey);
    }

    public static String rsaPublicKeyToString(PublicKey publicKey) {
        if (!ASYMMETRIC_ALGORITHM.equals(publicKey.getAlgorithm())) {
            throw new IllegalArgumentException("Public key must be RSA");
        }
        return "-----BEGIN RSA PUBLIC KEY-----\n" + MIME_ENCODER.encodeToString(publicKey.getEncoded()) + "\n-----END RSA PUBLIC KEY-----\n";
    }

    public static String pemRsaPrivateKeyToString(PrivateKey privateKey) {
        if (!ASYMMETRIC_ALGORITHM.equals(privateKey.getAlgorithm())) {
            throw new IllegalArgumentException("Private key must be RSA");
        }
        return "-----BEGIN RSA PRIVATE KEY-----\n" + MIME_ENCODER.encodeToString(privateKey.getEncoded()) + "\n-----END RSA PRIVATE KEY-----\n";
    }

    private static PrivateKey byteToPrivateKey(byte[] keyData) throws CryptException {
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyData);
            KeyFactory keyFactory = KeyFactory.getInstance(ASYMMETRIC_ALGORITHM);
            return keyFactory.generatePrivate(keySpec);
        }
        catch (Exception e) {
            throw new CryptException(e);
        }
    }

    public static PublicKey byteToPublicKey(byte[] keyData) throws CryptException {
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyData);
            KeyFactory keyFactory = KeyFactory.getInstance(ASYMMETRIC_ALGORITHM);
            return keyFactory.generatePublic(keySpec);
        }
        catch (Exception e) {
            throw new CryptException(e);
        }
    }

    public static SecretKey decryptByteToSecretKey(PrivateKey privateKey, byte[] keyData) throws CryptException {
        byte[] key = Crypt.decryptUsingKey(privateKey, keyData);
        try {
            return new SecretKeySpec(key, SYMMETRIC_ALGORITHM);
        }
        catch (Exception e) {
            throw new CryptException(e);
        }
    }

    public static byte[] encryptUsingKey(Key key, byte[] input) throws CryptException {
        return Crypt.cipherData(1, key, input);
    }

    public static byte[] decryptUsingKey(Key key, byte[] input) throws CryptException {
        return Crypt.cipherData(2, key, input);
    }

    private static byte[] cipherData(int cipherOpMode, Key key, byte[] input) throws CryptException {
        try {
            return Crypt.setupCipher(cipherOpMode, key.getAlgorithm(), key).doFinal(input);
        }
        catch (Exception e) {
            throw new CryptException(e);
        }
    }

    private static Cipher setupCipher(int cipherOpMode, String algorithm, Key key) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(cipherOpMode, key);
        return cipher;
    }

    public static Cipher getCipher(int opMode, Key key) throws CryptException {
        try {
            Cipher cip = Cipher.getInstance("AES/CFB8/NoPadding");
            cip.init(opMode, key, new IvParameterSpec(key.getEncoded()));
            return cip;
        }
        catch (Exception e) {
            throw new CryptException(e);
        }
    }

    private static interface ByteArrayToKeyFunction<T extends Key> {
        public T apply(byte[] var1) throws CryptException;
    }

    public record SaltSignaturePair(long salt, byte[] signature) {
        public static final SaltSignaturePair EMPTY = new SaltSignaturePair(0L, ByteArrays.EMPTY_ARRAY);

        public SaltSignaturePair(FriendlyByteBuf input) {
            this(input.readLong(), input.readByteArray());
        }

        public boolean isValid() {
            return this.signature.length > 0;
        }

        public static void write(FriendlyByteBuf output, SaltSignaturePair saltSignaturePair) {
            output.writeLong(saltSignaturePair.salt);
            output.writeByteArray(saltSignaturePair.signature);
        }

        public byte[] saltAsBytes() {
            return Longs.toByteArray((long)this.salt);
        }
    }

    public static class SaltSupplier {
        private static final SecureRandom secureRandom = new SecureRandom();

        public static long getLong() {
            return secureRandom.nextLong();
        }
    }
}

