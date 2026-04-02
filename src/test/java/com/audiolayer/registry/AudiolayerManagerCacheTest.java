package com.audiolayer.registry;

import com.audiolayer.api.SoundId;
import com.audiolayer.audio.HashService;
import com.audiolayer.audio.InputAudioScanner;
import com.audiolayer.cache.JsonCacheIndexRepository;
import com.audiolayer.config.AudiolayerConfig;
import com.audiolayer.config.FilenameSanitizer;
import com.audiolayer.config.SoundIdMapper;
import com.audiolayer.testsupport.TestAssertions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public final class AudiolayerManagerCacheTest {
    public static void run() throws Exception {
        Path root = Files.createTempDirectory("audiolayer-cache");
        Path input = root.resolve("input");
        Path cache = root.resolve("cache");
        Files.createDirectories(input);
        Files.writeString(input.resolve("track.mp3"), "data");

        AudiolayerConfig config = new AudiolayerConfig(input, cache, true);
        InputAudioScanner scanner = new InputAudioScanner(new SoundIdMapper(new FilenameSanitizer()), new HashService());
        JsonCacheIndexRepository repo = new JsonCacheIndexRepository(cache.resolve("index.json"));
        AtomicInteger durationCallCount = new AtomicInteger();
        AudiolayerManager manager = new AudiolayerManager(
                config, scanner, repo,
                path -> { durationCallCount.incrementAndGet(); return 120f; }
        );

        // first reload — duration read runs, asset loaded
        var s1 = manager.reload();
        TestAssertions.assertEquals(1, s1.loadedAssets());
        TestAssertions.assertEquals(0, s1.reusedAssets());
        SoundId id = new SoundId("audiolayer", "track");
        TestAssertions.assertTrue(manager.isLoaded(id));
        int callsAfterFirst = durationCallCount.get();
        TestAssertions.assertEquals(1, callsAfterFirst);

        // second reload — cache index hit → duration reused, no re-read
        var s2 = manager.reload();
        TestAssertions.assertEquals(1, s2.reusedAssets());
        TestAssertions.assertEquals(0, s2.failedFiles());
        TestAssertions.assertEquals(callsAfterFirst, durationCallCount.get());
    }
}
