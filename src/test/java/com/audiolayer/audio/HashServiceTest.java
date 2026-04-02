package com.audiolayer.audio;

import com.audiolayer.testsupport.TestAssertions;

import java.nio.file.Files;
import java.nio.file.Path;

public final class HashServiceTest {
    public static void run() throws Exception {
        HashService hashService = new HashService();
        Path file = Files.createTempFile("audiolayer", ".bin");
        Files.writeString(file, "hello");
        String first = hashService.hash(file);
        String second = hashService.hash(file);
        TestAssertions.assertEquals(first, second);
    }
}
