package com.audiolayer.registry;

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
        FakeConversionService conversion = new FakeConversionService(true);
        AudiolayerManager manager = new AudiolayerManager(
                config, scanner, repo, new AudioRegistryService(), conversion,
                new com.audiolayer.resource.RuntimeResourcePackWriter(cache.resolve("pack"))
        );

        // first reload — conversion runs, asset loaded
        var s1 = manager.reload();
        TestAssertions.assertEquals(1, s1.loadedAssets());
        TestAssertions.assertEquals(0, s1.reusedAssets());
        SoundId id = new SoundId("audiolayer", "track");
        TestAssertions.assertTrue(manager.isLoaded(id));

        // second reload — cache index hit, file exists → reused, no reconversion
        int callsBefore = conversion.callCount();
        var s2 = manager.reload();
        TestAssertions.assertEquals(1, s2.reusedAssets());
        TestAssertions.assertEquals(0, s2.failedFiles());
        TestAssertions.assertEquals(callsBefore, conversion.callCount());

        // delete the cache file — should force reconversion even though index has it
        Files.delete(manager.get(id).orElseThrow().cacheFile());
        var s3 = manager.reload();
        TestAssertions.assertEquals(1, s3.loadedAssets());
        TestAssertions.assertEquals(0, s3.reusedAssets());
        TestAssertions.assertTrue(conversion.callCount() > callsBefore);
    }
}
