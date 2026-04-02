package com.audiolayer.registry;

import com.audiolayer.api.ReloadSummary;
import com.audiolayer.api.SoundId;
import com.audiolayer.audio.HashService;
import com.audiolayer.audio.InputAudioScanner;
import com.audiolayer.cache.JsonCacheIndexRepository;
import com.audiolayer.config.AudiolayerConfig;
import com.audiolayer.config.FilenameSanitizer;
import com.audiolayer.config.SoundIdMapper;
import com.audiolayer.conversion.FakeConversionService;
import com.audiolayer.testsupport.TestAssertions;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AudiolayerManagerTest {
    public static void run() throws Exception {
        Path root = Files.createTempDirectory("audiolayer-core");
        Path input = root.resolve("input");
        Path cache = root.resolve("cache");
        Files.createDirectories(input.resolve("music"));
        Files.writeString(input.resolve("music/hello.mp3"), "hello");

        AudiolayerConfig config = new AudiolayerConfig(input, cache, true);
        InputAudioScanner scanner = new InputAudioScanner(new SoundIdMapper(new FilenameSanitizer()), new HashService());
        AudiolayerManager manager = new AudiolayerManager(
                config,
                scanner,
                new JsonCacheIndexRepository(cache.resolve("index.json")),
                new AudioRegistryService(),
                new FakeConversionService(true),
                new com.audiolayer.resource.RuntimeResourcePackWriter(cache.resolve("runtime-pack"))
        );

        ReloadSummary summary = manager.reload();

        TestAssertions.assertEquals(1, summary.scannedFiles());
        TestAssertions.assertEquals(1, summary.loadedAssets());
        TestAssertions.assertTrue(manager.isLoaded(new SoundId("audiolayer", "music.hello")));
    }
}
