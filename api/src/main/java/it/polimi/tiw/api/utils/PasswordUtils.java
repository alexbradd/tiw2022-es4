package it.polimi.tiw.api.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Set of utilities for hashing and verifying passwords
 */
public class PasswordUtils {
    /**
     * Hashes and salts the given password.
     *
     * @param password the cleartext password
     * @return hash + salt
     * @throws NullPointerException if {@code password} is null
     */
    public static String toHash(String password) {
        Objects.requireNonNull(password);
        byte[] salt = genSalt();
        return genDigest(password) + digestSalt(salt) + ':' + toHex(salt);
    }

    /**
     * Returns true if the given hash + salt string matches the given clear one.
     *
     * @param hashed the hash + salt string
     * @param clear  the clear string
     * @return true if the given hash + salt string matches the given clear one.
     * @throws NullPointerException if any parameter is null
     */
    public static boolean match(String hashed, String clear) {
        Objects.requireNonNull(hashed);
        Objects.requireNonNull(clear);
        String salt = hashed.substring(hashed.lastIndexOf(':'));
        String hashedClear = genDigest(clear) + digestSalt(fromHex(salt)) + ':' + salt;
        return Objects.equals(hashed, hashedClear);
    }

    /**
     * Generates a random salt
     */
    private static byte[] genSalt() {
        SecureRandom r = new SecureRandom();
        byte[] salt = new byte[64];
        r.nextBytes(salt);
        return salt;
    }

    /**
     * Hashes with SHA-256 the string
     */
    private static String genDigest(String s) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(s.getBytes());
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("AAAAA", e);
        }
    }

    /**
     * Converts the byte array into a string of hex numbers
     */
    private static String toHex(byte[] array) {
        return HexFormat.of().formatHex(array);
    }

    private static byte[] fromHex(String s) {
        return HexFormat.of().parseHex(s);
    }

    /**
     * Calculates the hash of the salt
     */
    private static String digestSalt(byte[] salt) {
        try {
            byte[] copy = salt.clone();
            MessageDigest.getInstance("SHA-256").update(copy);
            return toHex(copy);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("AAAAA", e);
        }
    }
}
