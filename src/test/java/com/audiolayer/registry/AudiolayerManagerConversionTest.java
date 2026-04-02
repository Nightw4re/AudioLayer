package com.audiolayer.registry;

import com.audiolayer.audio.HashService;
import com.audiolayer.audio.InputAudioScanner;
import com.audiolayer.cache.JsonCacheIndexRepository;
import com.audiolayer.config.AudiolayerConfig;
import com.audiolayer.config.FilenameSanitizer;
import com.audiolayer.config.SoundIdMapper;
import com.audiolayer.testsupport.TestAssertions;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AudiolayerManagerConversionTest {
    public static void run() throws Exception {
        Path root = Files.createTempDirectory("audiolayer-conversion");
        Path input = root.resolve("input");
        Path cache = root.resolve("cache");
        Files.createDirectories(input.resolve("music"));
        Files.writeString(input.resolve("music/track.mp3"), "hello");

        AudiolayerConfig config = new AudiolayerConfig(input, cache, true);
        InputAudioScanner scanner = new InputAudioScanner(new SoundIdMapper(new FilenameSanitizer()), new HashService());
        AudiolayerManager manager = new AudiolayerManager(
                config, scanner,
                new JsonCacheIndexRepository(cache.resolve("index.json")),
                path -> 42f
        );

        var summary = manager.reload();

        TestAssertions.assertEquals(1, summary.scannedFiles());
        TestAssertions.assertEquals(1, summary.loadedAssets());
        TestAssertions.assertEquals(0, summary.failedFiles());

        var asset = manager.get(new com.audiolayer.api.SoundId("audiolayer", "music.track")).orElseThrow();
        TestAssertions.assertEquals(42f, asset.durationSeconds());
        TestAssertions.assertTrue(Files.exists(asset.sourceFile()));
    }
}
