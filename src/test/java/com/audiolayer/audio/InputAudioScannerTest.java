package com.audiolayer.audio;

import com.audiolayer.config.FilenameSanitizer;
import com.audiolayer.config.SoundIdMapper;
import com.audiolayer.testsupport.TestAssertions;

import java.nio.file.Files;
import java.nio.file.Path;

public final class InputAudioScannerTest {
    public static void run() throws Exception {
        Path root = Files.createTempDirectory("audiolayer-input");
        Files.createDirectories(root.resolve("music"));
        Files.writeString(root.resolve("music/atlantis_intro.mp3"), "a");
        Files.writeString(root.resolve("music/ignore.txt"), "b");

        InputAudioScanner scanner = new InputAudioScanner(new SoundIdMapper(new FilenameSanitizer()), new HashService());
        var descriptors = scanner.scan(root);

        TestAssertions.assertEquals(1, descriptors.size());
        TestAssertions.assertEquals("audiolayer:music.atlantis_intro", descriptors.get(0).soundId().toString());
    }
}
