package it.polimi.tiw.api.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Objects;

public class IdUtils {
    /**
     * Converts the given long to an url-safe base64 string.
     *
     * @param id the long to convert
     * @return an url-safe base64 string.
     */
    public static String toBase64(long id) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(longToByteArray(id));
    }

    /**
     * Converts the given url-safe base64 string and convert it to a long.
     *
     * @param id the base64 string
     * @return a long
     * @throws NullPointerException     if {@code id} is null
     * @throws IllegalArgumentException if {@code id} is not a valid base64 string
     */
    public static long fromBase64(String id) {
        Objects.requireNonNull(id);
        return byteArrayToLong(Base64.getUrlDecoder().decode(id));
    }

    /**
     * Converts long to byte array
     */
    private static byte[] longToByteArray(long l) {
        try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
            try (DataOutputStream d = new DataOutputStream(s)) {
                d.writeLong(l);
                d.flush();
                return s.toByteArray();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    /**
     * Converts a byte array into a long
     */
    private static long byteArrayToLong(byte[] longBytes) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
        byteBuffer.put(longBytes);
        byteBuffer.flip();
        try {
            return byteBuffer.getLong();
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Sequence too short for long", e);
        }
    }
}
