package it.polimi.tiw.api.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilsTest {

    @Test
    void testMatch() {
        String hashed = PasswordUtils.toHash("password");
        assertTrue(PasswordUtils.match(hashed, "password"));
    }
}