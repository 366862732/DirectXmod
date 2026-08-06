/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.jsonrpc.security;

import java.security.SecureRandom;

public record SecurityConfig(String secretKey) {
    private static final String SECRET_KEY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static boolean isValid(String secretKey) {
        if (secretKey.isEmpty()) {
            return false;
        }
        return secretKey.matches("^[a-zA-Z0-9]{40}$");
    }

    public static String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        StringBuilder key = new StringBuilder(40);
        for (int i = 0; i < 40; ++i) {
            key.append(SECRET_KEY_CHARS.charAt(random.nextInt(SECRET_KEY_CHARS.length())));
        }
        return key.toString();
    }
}

