package com.onetattva.infron.core.common;

import java.util.UUID;

public class UuidUtils {
    /**
     * Validates if the given string is a valid UUID.
     * @param uuidString The string to check.
     * @return true if the string is a valid UUID, false otherwise.
     */
    public static boolean isValidUUID(String uuidString) {
        if (uuidString == null) {
            return false;
        }
        try {
            UUID.fromString(uuidString);
            return true;
        } catch (IllegalArgumentException exception) {
            // The string is not a valid UUID format
            return false;
        }
    }
}
