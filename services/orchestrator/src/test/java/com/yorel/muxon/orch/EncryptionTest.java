package com.yorel.muxon.orch;

import com.yorel.muxon.common.EncryptionUtil;
import com.yorel.muxon.common.MuxonCryptoConfigUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptionTest {

    @Test
    public void testEncryptionDecryption() {
        // Test with current configuration
        String instanceName = "local";
        int instanceId = 19;
        String passphrase = System.getenv("MUXON_PASSPHRASE");
        
        if (passphrase == null) {
            System.out.println("MUXON_PASSPHRASE not set, skipping test");
            return;
        }
        
        System.out.println("Using passphrase: " + passphrase);
        
        // Initialize encryption
        MuxonCryptoConfigUtil.initEncryptionKey(instanceName, instanceId);
        
        // Test basic encryption/decryption
        String testValue = "testPassword123";
        String encrypted = EncryptionUtil.encrypt(testValue);
        String decrypted = EncryptionUtil.decrypt(encrypted);
        
        System.out.println("Original: " + testValue);
        System.out.println("Encrypted: " + encrypted);
        System.out.println("Decrypted: " + decrypted);
        
        assertEquals(testValue, decrypted);
        
        // Try to decrypt the actual password from config
        String configPassword = "Oln5QcC7iHiLOtVIrfXzbQ==";
        try {
            String decryptedConfigPassword = EncryptionUtil.decrypt(configPassword);
            System.out.println("Config password decrypted successfully: " + decryptedConfigPassword);
        } catch (Exception e) {
            System.err.println("Failed to decrypt config password: " + e.getMessage());
            
            // Try different instance IDs that might have been used
            for (int testId = 1; testId <= 20; testId++) {
                try {
                    MuxonCryptoConfigUtil.initEncryptionKey(instanceName, testId);
                    String decryptedTest = EncryptionUtil.decrypt(configPassword);
                    System.out.println("SUCCESS with instanceId " + testId + ": " + decryptedTest);
                    break;
                } catch (Exception ex) {
                    // Continue trying
                }
            }
        }
    }
}
