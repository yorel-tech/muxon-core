package com.onetattva.infron.core.common;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
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

    /**
     * Generates a UUID v5 based on the given namespace and name.
     * @param namespace The namespace UUID.
     * @param name The name string.
     * @return The generated UUID v5.
     */
    public static UUID generateUuid5(UUID namespace, String name) {
        NameBasedGenerator gen = Generators.nameBasedGenerator(namespace);
        return gen.generate(name);
    }
}
