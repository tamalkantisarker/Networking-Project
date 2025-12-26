package com.securechat.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptoUtil {
    private static final String RSA_ALGO = "RSA";
    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12; // 12 bytes recommended for GCM
    private static final int GCM_TAG_LENGTH = 128;

    // RSA Generation
    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGO);
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    // RSA Encrypt (Public Key)
    public static byte[] encryptRSA(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    // RSA Decrypt (Private Key)
    public static byte[] decryptRSA(byte[] data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    // AES-GCM Key Generation
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }

    // AES-GCM Encrypt -> Returns IV + Ciphertext (envelope)
    public static byte[] encryptAES(byte[] data, SecretKey key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_ALGO);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

        byte[] ciphertext = cipher.doFinal(data);

        // Concatenate IV + Ciphertext
        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
        return result;
    }

    // AES-GCM Decrypt -> Expects IV + Ciphertext
    public static byte[] decryptAES(byte[] encryptedData, SecretKey key) throws Exception {
        if (encryptedData.length < GCM_IV_LENGTH)
            throw new IllegalArgumentException("Invalid encrypted data length");

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);

        int ciphertextLength = encryptedData.length - GCM_IV_LENGTH;
        byte[] ciphertext = new byte[ciphertextLength];
        System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertext, 0, ciphertextLength);

        Cipher cipher = Cipher.getInstance(AES_ALGO);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

        return cipher.doFinal(ciphertext);
    }

    // Diffie-Hellman Key Exchange Support
    public static KeyPair generateDHKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    public static SecretKey deriveSharedSecret(PrivateKey privateKey, byte[] otherPublicKeyBytes) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        PublicKey otherPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(otherPublicKeyBytes));

        KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
        keyAgree.init(privateKey);
        keyAgree.doPhase(otherPublicKey, true);

        byte[] sharedSecret = keyAgree.generateSecret();
        // Use SHA-256 to derive a 256-bit AES key from the secret
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(sharedSecret);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
