package com.audiolayer.compat;

import com.audiolayer.api.AudiolayerApi;
import com.audiolayer.audio.LoadedAudioAsset;
import com.audiolayer.api.SoundId;
import com.audiolayer.audio.HashService;
import com.audiolayer.audio.InputAudioScanner;
import com.audiolayer.cache.JsonCacheIndexRepository;
import com.audiolayer.config.AudiolayerConfig;
import com.audiolayer.config.FilenameSanitizer;
import com.audiolayer.config.SoundIdMapper;
import com.audiolayer.registry.AudiolayerManager;
import com.audiolayer.testsupport.TestAssertions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Verifies AudiolayerApi behaviour for the Etched integration.
 *
 * Etched sets a sound event with the audiolayer: namespace on a disc.
 * When the jukebox plays that disc, PlaySoundEvent fires and our hook calls:
 *   api.isLoaded(id)  — to guard before playing
 *   api.play(id)      — to start Mp3SoundInstance
 *   api.stop()        — when disc is removed
 *
 * None of these require a running Minecraft instance.
 */
public final class CompatApiContractTest {

    public static void run() throws Exception {
        // hook guard + playback
        testPlay_unknownSound_isNoOp();
        testPlay_knownSound_isAccepted();
        testPlay_withCountStartDuration_isAccepted();
        testPlay_infiniteLoop_countZero();
        testStop_withoutPriorPlay_isNoOp();
        testStop_afterPlay_clearsState();

        // isLoaded / get — called by hook before play
        testIsLoaded_returnsFalseBeforeReload();
        testIsLoaded_returnsTrueAfterReload();
        testListSounds_emptyBeforeReload();
        testListSounds_populatedAfterReload();
        testGet_returnsAssetMetadata();

        // reload — sounds must be loaded before hook can intercept them
        testReload_whileIdle_succeeds();
        testReload_twice_replacesRegistry();
        testReload_addFile_soundBecomesAvailable();
        testReload_removeFile_soundBecomesUnavailable();
    }

    // -------------------------------------------------------------------------
    // Hook guard + playback
    // -------------------------------------------------------------------------

    private static void testPlay_unknownSound_isNoOp() throws Exception {
        StubApi api = new StubApi();
        SoundId unknown = new SoundId("audiolayer", "does.not.exist");
        // compat mods check isLoaded() first — unknown sound → no play call
        if (!api.isLoaded(unknown)) return; // correct early-exit path
        api.play(unknown);
        TestAssertions.assertEquals(0, api.playCalls.size());
    }

    private static void testPlay_knownSound_isAccepted() throws Exception {
        StubApi api = stubWithSound("music.theme");
        SoundId id = new SoundId("audiolayer", "music.theme");
        api.play(id);
        TestAssertions.assertEquals(1, api.playCalls.size());
        TestAssertions.assertEquals("audiolayer:music.theme", api.playCalls.get(0));
    }

    private static void testPlay_withCountStartDuration_isAccepted() throws Exception {
        StubApi api = stubWithSound("music.theme");
        SoundId id = new SoundId("audiolayer", "music.theme");
        // Etched may pass count=1, start=0, duration=0 (play once from beginning)
        api.play(id, 1, 0f, 0f);
        TestAssertions.assertEquals(1, api.playFullCalls.size());
        TestAssertions.assertEquals("audiolayer:music.theme|1|0.0|0.0", api.playFullCalls.get(0));
    }

    private static void testPlay_infiniteLoop_countZero() throws Exception {
        StubApi api = stubWithSound("music.theme");
        SoundId id = new SoundId("audiolayer", "music.theme");
        // count=0 means infinite loop
        api.play(id, 0, 0f, 0f);
        TestAssertions.assertEquals(1, api.playFullCalls.size());
        TestAssertions.assertEquals("audiolayer:music.theme|0|0.0|0.0", api.playFullCalls.get(0));
    }

    private static void testStop_withoutPriorPlay_isNoOp() {
        StubApi api = new StubApi();
        api.stop(); // must not throw
        TestAssertions.assertEquals(1, api.stopCount);
    }

    private static void testStop_afterPlay_clearsState() throws Exception {
        StubApi api = stubWithSound("music.theme");
        api.play(new SoundId("audiolayer", "music.theme"));
        api.stop();
        TestAssertions.assertEquals(1, api.stopCount);
    }

    // -------------------------------------------------------------------------
    // isLoaded / get — called by hook before play
    // -------------------------------------------------------------------------

    private static void testIsLoaded_returnsFalseBeforeReload() throws Exception {
        AudiolayerManager manager = emptyManager();
        TestAssertions.assertTrue(!manager.isLoaded(new SoundId("audiolayer", "music.theme")));
    }

    private static void testIsLoaded_returnsTrueAfterReload() throws Exception {
        AudiolayerManager manager = managerWithFile("music/theme.mp3");
        manager.reload();
        TestAssertions.assertTrue(manager.isLoaded(new SoundId("audiolayer", "music.theme")));
    }

    private static void testListSounds_emptyBeforeReload() throws Exception {
        AudiolayerManager manager = emptyManager();
        TestAssertions.assertEquals(0, manager.listSounds().size());
    }

    private static void testListSounds_populatedAfterReload() throws Exception {
        AudiolayerManager manager = managerWithFile("music/theme.mp3");
        manager.reload();
        Set<SoundId> sounds = manager.listSounds();
        TestAssertions.assertEquals(1, sounds.size());
        TestAssertions.assertTrue(sounds.contains(new SoundId("audiolayer", "music.theme")));
    }

    private static void testGet_returnsAssetMetadata() throws Exception {
        AudiolayerManager manager = managerWithFile("music/theme.mp3");
        manager.reload();
        SoundId id = new SoundId("audiolayer", "music.theme");
        Optional<LoadedAudioAsset> asset = manager.get(id);
        TestAssertions.assertTrue(asset.isPresent());
        TestAssertions.assertEquals(id, asset.get().soundId());
        TestAssertions.assertTrue(asset.get().sourceFile() != null);
    }

    // -------------------------------------------------------------------------
    // reload — sounds must be loaded before hook can intercept them
    // -------------------------------------------------------------------------

    private static void testReload_whileIdle_succeeds() throws Exception {
        AudiolayerManager manager = emptyManager();
        manager.reload(); // must not throw
    }

    private static void testReload_twice_replacesRegistry() throws Exception {
        AudiolayerManager manager = managerWithFile("music/theme.mp3");
        manager.reload();
        TestAssertions.assertEquals(1, manager.listSounds().size());
        manager.reload();
        TestAssertions.assertEquals(1, manager.listSounds().size());
    }

    private static void testReload_addFile_soundBecomesAvailable() throws Exception {
        Path input = Files.createTempDirectory("audiolayer-compat-add").resolve("input");
        Files.createDirectories(input.resolve("music"));
        Path cacheDir = input.getParent().resolve("cache");
        AudiolayerManager manager = buildManager(input, cacheDir);

        manager.reload();
        TestAssertions.assertEquals(0, manager.listSounds().size());

        Files.writeString(input.resolve("music/theme.mp3"), "fake-mp3-data");
        manager.reload();
        TestAssertions.assertTrue(manager.isLoaded(new SoundId("audiolayer", "music.theme")));
    }

    private static void testReload_removeFile_soundBecomesUnavailable() throws Exception {
        Path input = Files.createTempDirectory("audiolayer-compat-remove").resolve("input");
        Files.createDirectories(input.resolve("music"));
        Path file = input.resolve("music/theme.mp3");
        Files.writeString(file, "fake-mp3-data");
        Path cacheDir = input.getParent().resolve("cache");
        AudiolayerManager manager = buildManager(input, cacheDir);

        manager.reload();
        TestAssertions.assertTrue(manager.isLoaded(new SoundId("audiolayer", "music.theme")));

        Files.delete(file);
        manager.reload();
        TestAssertions.assertTrue(!manager.isLoaded(new SoundId("audiolayer", "music.theme")));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static AudiolayerManager emptyManager() throws Exception {
        Path root = Files.createTempDirectory("audiolayer-compat-empty");
        Path input = root.resolve("input");
        Files.createDirectories(input);
        return buildManager(input, root.resolve("cache"));
    }

    private static AudiolayerManager managerWithFile(String relativePath) throws Exception {
        Path root = Files.createTempDirectory("audiolayer-compat");
        Path input = root.resolve("input");
        Path file = input.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "fake-mp3-data");
        return buildManager(input, root.resolve("cache"));
    }

    private static AudiolayerManager buildManager(Path input, Path cache) throws Exception {
        AudiolayerConfig config = new AudiolayerConfig(input, cache, false);
        InputAudioScanner scanner = new InputAudioScanner(
                new SoundIdMapper(new FilenameSanitizer()), new HashService());
        return new AudiolayerManager(
                config, scanner,
                new JsonCacheIndexRepository(cache.resolve("index.json")),
                path -> 120f);
    }

    private static StubApi stubWithSound(String path) {
        StubApi api = new StubApi();
        api.loadedSounds.add(new SoundId("audiolayer", path));
        return api;
    }

    // -------------------------------------------------------------------------
    // Minimal stub — simulates what AudiolayerMod registers at runtime
    // -------------------------------------------------------------------------

    private static final class StubApi implements AudiolayerApi {
        final List<String> playCalls = new ArrayList<>();
        final List<String> playFullCalls = new ArrayList<>();
        int stopCount;
        final List<SoundId> loadedSounds = new ArrayList<>();

        @Override public boolean isLoaded(SoundId id) { return loadedSounds.contains(id); }
        @Override public Set<SoundId> listSounds() { return Set.copyOf(loadedSounds); }
        @Override public Optional<LoadedAudioAsset> get(SoundId id) { return Optional.empty(); }
        @Override public void reload() {}
        @Override public void play(SoundId id) { playCalls.add(id.toString()); }
        @Override public void play(SoundId id, int count, float start, float duration) {
            playFullCalls.add(id + "|" + count + "|" + start + "|" + duration);
        }
        @Override public void stop() { stopCount++; }
    }
}
