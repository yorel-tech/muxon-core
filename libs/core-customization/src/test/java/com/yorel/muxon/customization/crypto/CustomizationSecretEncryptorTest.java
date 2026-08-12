/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.customization.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CustomizationSecretEncryptorTest {

  private static final String VALID_KEY = CustomizationSecretEncryptor.generateKey();

  @Test
  void encryptDecrypt_roundTrip() {
    CustomizationSecretEncryptor enc = new CustomizationSecretEncryptor(VALID_KEY);
    String plaintext = "P@ssw0rd!";

    String ciphertext = enc.encrypt(plaintext);
    assertNotNull(ciphertext, "ciphertext must not be null");
    assertNotEquals(plaintext, ciphertext, "ciphertext must differ from plaintext");

    String decrypted = enc.decrypt(ciphertext);
    assertEquals(plaintext, decrypted, "decrypt(encrypt(x)) must equal x");
  }

  @Test
  void encryptTwice_producesDifferentCiphertexts() {
    CustomizationSecretEncryptor enc = new CustomizationSecretEncryptor(VALID_KEY);
    String ct1 = enc.encrypt("secret");
    String ct2 = enc.encrypt("secret");

    assertNotEquals(
        ct1,
        ct2,
        "each encryption must use a fresh IV — two encryptions of the same plaintext must differ");
  }

  @Test
  void encryptNull_returnsNull() {
    CustomizationSecretEncryptor enc = new CustomizationSecretEncryptor(VALID_KEY);
    assertNull(enc.encrypt(null));
  }

  @Test
  void decryptNull_returnsNull() {
    CustomizationSecretEncryptor enc = new CustomizationSecretEncryptor(VALID_KEY);
    assertNull(enc.decrypt(null));
  }

  @Test
  void invalidKeyLength_throwsIllegalArgument() {
    // A key that is not 32 bytes
    String shortKey = java.util.Base64.getEncoder().encodeToString(new byte[16]);
    assertThrows(
        IllegalArgumentException.class,
        () -> new CustomizationSecretEncryptor(shortKey),
        "must reject keys shorter than 32 bytes");
  }

  @Test
  void decryptWithWrongKey_throwsIllegalState() {
    String key2 = CustomizationSecretEncryptor.generateKey();
    CustomizationSecretEncryptor enc1 = new CustomizationSecretEncryptor(VALID_KEY);
    CustomizationSecretEncryptor enc2 = new CustomizationSecretEncryptor(key2);

    String ciphertext = enc1.encrypt("secret");
    assertThrows(
        IllegalStateException.class,
        () -> enc2.decrypt(ciphertext),
        "decryption with the wrong key must throw");
  }

  @Test
  void generateKey_produces32ByteBase64() {
    String key = CustomizationSecretEncryptor.generateKey();
    assertNotNull(key);
    byte[] decoded = java.util.Base64.getDecoder().decode(key);
    assertEquals(32, decoded.length, "generateKey must produce a 32-byte (256-bit) key");
  }

  @Test
  void encryptDecrypt_emptyString() {
    CustomizationSecretEncryptor enc = new CustomizationSecretEncryptor(VALID_KEY);
    String ciphertext = enc.encrypt("");
    assertNotNull(ciphertext);
    assertEquals("", enc.decrypt(ciphertext), "empty string must round-trip correctly");
  }

  @Test
  void encryptDecrypt_unicodePassword() {
    CustomizationSecretEncryptor enc = new CustomizationSecretEncryptor(VALID_KEY);
    String unicode = "P@\u00e4ssw\u00f6rd\u00df123";
    assertEquals(
        unicode,
        enc.decrypt(enc.encrypt(unicode)),
        "unicode passwords must survive encrypt/decrypt");
  }
}
