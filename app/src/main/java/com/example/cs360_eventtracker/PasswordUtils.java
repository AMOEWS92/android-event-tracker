package com.example.cs360_eventtracker;

import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtils {

    private static final int ITERATIONS = 210000;
    private static final int KEY_LENGTH = 256;

    private PasswordUtils() {

    }

    // Generate a random salt for each account
    public static String generateSalt() {

        byte[] salt = new byte[16];

        new SecureRandom().nextBytes(salt);

        return Base64.encodeToString(
                salt, Base64.NO_WRAP
        );
    }

    // Hash a password using PBKDF2
    public static String hashPassword(
            String password, String saltString) {

        byte[] salt = Base64.decode(
                saltString, Base64.DEFAULT
        );

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            byte[] hash = factory
                    .generateSecret(spec)
                    .getEncoded();

            return Base64.encodeToString(
                    hash, Base64.NO_WRAP
            );
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Unable to hash password", e
            );

        } finally {
            spec.clearPassword();
        }
    }

    // Verify a password against its stored hash
    public static boolean verifyPassword(
            String password,
            String salt,
            String storedHash) {

        String calculatedHash =
                hashPassword(password, salt);

        byte[] enteredBytes = Base64.decode(
                calculatedHash, Base64.DEFAULT
        );

        byte[] storedBytes = Base64.decode(
                storedHash, Base64.DEFAULT
        );

        return MessageDigest.isEqual(
                enteredBytes, storedBytes
        );
    }
}