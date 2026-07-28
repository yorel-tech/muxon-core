package com.scal.muxon.customization.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM field-level encryption for customization secrets (passwords).
 *
 * <p>Cipher text format (Base64 of): {@code [12-byte IV][ciphertext+16-byte tag]}.
 *
 * <p>The master key is injected at startup (typically from an env var or secrets manager).
 * The key must be a 32-byte (256-bit) value encoded as a Base64 string.
 *
 * <p><strong>Usage in VmsService:</strong> call {@link #encrypt(String)} before persisting
 * passwords to the DB. The worker calls {@link #decrypt(String)} after loading from DB.
 */
public class CustomizationSecretEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom rng = new SecureRandom();

    /**
     * @param base64Key 32-byte AES key encoded as standard Base64.
     */
    public CustomizationSecretEncryptor(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "Customization encryption key must be exactly 32 bytes (256-bit AES); got " + keyBytes.length);
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    /** Encrypt a plaintext secret. Returns a Base64-encoded ciphertext blob. */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_BYTES];
            rng.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] combined = new byte[IV_BYTES + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, IV_BYTES);
            System.arraycopy(cipherBytes, 0, combined, IV_BYTES, cipherBytes.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt customization secret", e);
        }
    }

    /** Decrypt a Base64-encoded ciphertext blob. Returns the plaintext. */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);
            byte[] encryptedBytes = new byte[combined.length - IV_BYTES];
            System.arraycopy(combined, IV_BYTES, encryptedBytes, 0, encryptedBytes.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encryptedBytes), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt customization secret", e);
        }
    }

    /** Generate a fresh random 32-byte key as a Base64 string (for setup/bootstrapping). */
    public static String generateKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256);
            return Base64.getEncoder().encodeToString(kg.generateKey().getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate AES key", e);
        }
    }
}
