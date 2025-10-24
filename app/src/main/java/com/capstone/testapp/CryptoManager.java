package com.capstone.testapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class CryptoManager {
    private static final String TAG = "CryptoManager";
    private static final String ALGORITHM = "RSA";
    private static final String PREFS_NAME = "CryptoPrefs";
    private static final String PUBLIC_KEY_PREF = "public_key";
    private static final String PRIVATE_KEY_PREF = "private_key";

    private SharedPreferences sharedPreferences;

    public CryptoManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Generate a new key pair and save it
    public void generateAndSaveKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            String publicKeyStr = Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT);
            String privateKeyStr = Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT);

            sharedPreferences.edit()
                    .putString(PUBLIC_KEY_PREF, publicKeyStr)
                    .putString(PRIVATE_KEY_PREF, privateKeyStr)
                    .apply();
            Log.d(TAG, "Keys generated and saved successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error generating keys", e);
        }
    }

    // Check if keys have already been generated
    public boolean areKeysGenerated() {
        return sharedPreferences.contains(PUBLIC_KEY_PREF);
    }

    // Get the user's own public key
    public String getPublicKeyString() {
        return sharedPreferences.getString(PUBLIC_KEY_PREF, null);
    }

    // Encrypt a message with a given public key
    public String encrypt(String plainText, String publicKeyStr) {
        try {
            byte[] publicKeyBytes = Base64.decode(publicKeyStr, Base64.DEFAULT);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting", e);
            return null;
        }
    }

    // Decrypt a message with the user's own private key
    public String decrypt(String encryptedText) {
        try {
            String privateKeyStr = sharedPreferences.getString(PRIVATE_KEY_PREF, null);
            if (privateKeyStr == null) return null;

            byte[] privateKeyBytes = Base64.decode(privateKeyStr, Base64.DEFAULT);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.decode(encryptedText, Base64.DEFAULT));
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting", e);
            return null;
        }
    }
}