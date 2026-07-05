package com.cipherlink.service;

import com.cipherlink.dto.*;
import com.cipherlink.model.*;
import com.cipherlink.repository.*;
import com.cipherlink.security.JwtService;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.prng.FixedSecureRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CryptoService {

    public String[] generateKeyPair() {
        try {
            X25519KeyPairGenerator keyGen = new X25519KeyPairGenerator();
            keyGen.init(new X25519KeyGenerationParameters(new SecureRandom()));
            var keyPair = keyGen.generateKeyPair();

            String pub = Base64.getEncoder().encodeToString(
                    ((X25519PublicKeyParameters) keyPair.getPublic()).getEncoded());
            String priv = Base64.getEncoder().encodeToString(
                    ((X25519PrivateKeyParameters) keyPair.getPrivate()).getEncoded());

            return new String[]{pub, priv};
        } catch (Exception e) {
            throw new RuntimeException("Key generation failed", e);
        }
    }

    public String performX3DH_Sender(String senderIdentityPriv, String senderEphemeralPriv,
                                     String receiverIdentityPub, String receiverSignedPreKeyPub,
                                     String receiverOneTimePreKeyPub) {
        byte[] dh1 = diffieHellman(senderIdentityPriv, receiverSignedPreKeyPub);
        byte[] dh2 = diffieHellman(senderEphemeralPriv, receiverIdentityPub);
        byte[] dh3 = diffieHellman(senderEphemeralPriv, receiverSignedPreKeyPub);
        byte[] dh4 = diffieHellman(senderEphemeralPriv, receiverOneTimePreKeyPub);

        byte[] masterSecret = concat(dh1, dh2, dh3, dh4);
        return Base64.getEncoder().encodeToString(deriveKey(masterSecret));
    }

    public String performX3DH_Receiver(String receiverIdentityPriv, String receiverSignedPreKeyPriv,
                                       String receiverOneTimePreKeyPriv, String senderIdentityPub,
                                       String senderEphemeralPub) {
        byte[] dh1 = diffieHellman(receiverSignedPreKeyPriv, senderIdentityPub);
        byte[] dh2 = diffieHellman(receiverIdentityPriv, senderEphemeralPub);
        byte[] dh3 = diffieHellman(receiverSignedPreKeyPriv, senderEphemeralPub);
        byte[] dh4 = diffieHellman(receiverOneTimePreKeyPriv, senderEphemeralPub);

        byte[] masterSecret = concat(dh1, dh2, dh3, dh4);
        return Base64.getEncoder().encodeToString(deriveKey(masterSecret));
    }

    public String[] encryptMessage(String plaintext, String base64Key) throws Exception {
        byte[] key = Arrays.copyOf(Base64.getDecoder().decode(base64Key), 32);
        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        return new String[]{
            Base64.getEncoder().encodeToString(ciphertext),
            Base64.getEncoder().encodeToString(nonce)
        };
    }

    public String decryptMessage(String base64Ciphertext, String base64Nonce, String base64Key) throws Exception {
        byte[] key = Arrays.copyOf(Base64.getDecoder().decode(base64Key), 32);
        byte[] nonce = Base64.getDecoder().decode(base64Nonce);
        byte[] ciphertext = Base64.getDecoder().decode(base64Ciphertext);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        byte[] plaintext = cipher.doFinal(ciphertext);

        return new String(plaintext, StandardCharsets.UTF_8);
    }

    public String signData(String base64Key, String data) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        byte[] keyMaterial = Arrays.copyOf(keyBytes, Math.min(keyBytes.length, 32));
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(keyMaterial, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(
                hmac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean verifySignature(String base64Key, String data, String base64Signature) throws Exception {
        return signData(base64Key, data).equals(base64Signature);
    }

    private byte[] diffieHellman(String base64Priv, String base64Pub) {
        try {
            X25519PrivateKeyParameters priv = new X25519PrivateKeyParameters(
                    Base64.getDecoder().decode(base64Priv));
            X25519PublicKeyParameters pub = new X25519PublicKeyParameters(
                    Base64.getDecoder().decode(base64Pub));

            X25519Agreement agreement = new X25519Agreement();
            agreement.init(priv);
            byte[] result = new byte[agreement.getAgreementSize()];
            agreement.calculateAgreement(pub, result, 0);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("DH failed", e);
        }
    }

    private byte[] deriveKey(byte[] inputKeyMaterial) {
        try {
            byte[] salt = new byte[32];
            byte[] info = "CipherLink_X3DH_v1".getBytes(StandardCharsets.UTF_8);
            // HKDF using BouncyCastle
            org.bouncycastle.crypto.generators.HKDFBytesGenerator hkdf =
                    new org.bouncycastle.crypto.generators.HKDFBytesGenerator(
                            new org.bouncycastle.crypto.digests.SHA256Digest());
            hkdf.init(new org.bouncycastle.crypto.params.HKDFParameters(inputKeyMaterial, salt, info));
            byte[] okm = new byte[32];
            hkdf.generateBytes(okm, 0, 32);
            return okm;
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }

    private byte[] concat(byte[]... arrays) {
        int totalLength = Arrays.stream(arrays).mapToInt(a -> a.length).sum();
        byte[] result = new byte[totalLength];
        int pos = 0;
        for (byte[] arr : arrays) {
            System.arraycopy(arr, 0, result, pos, arr.length);
            pos += arr.length;
        }
        return result;
    }
}
