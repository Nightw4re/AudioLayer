package com.audiolayer.audio;

import com.audiolayer.testsupport.TestAssertions;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public final class HashServiceStreamTest {
    public static void run() throws Exception {
        HashService hashService = new HashService();
        String hash = hashService.hash(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));
        TestAssertions.assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hash);
    }
}
