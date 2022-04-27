package it.polimi.tiw.api.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdUtilsTest {

    @Test
    void testBase64() {
        long id = 0L;

        String b64 = IdUtils.toBase64(id);
        assertEquals("AAAAAAAAAAA", b64);

        long l = IdUtils.fromBase64("AAAAAAAAAAA");
        assertEquals(0L, l);
    }

}