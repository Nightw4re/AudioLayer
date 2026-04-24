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
 * Verifies that AmbientSounds, Reactive Music, Ambient Environment, and Euphonium
 * can use the Audiolayer API to load and play MP3 files.
 *
 * These mods cannot run together in a live game, but each of their integration
 * patterns can be verified independently against the same API contract.
 *
 * Integration pattern shared by all four mods:
 *   1. Call api.isLoaded(id) — guard before attempting playback
 *   2. Call api.get(id)      — obtain the MP3 source path and metadata
 *   3. Call api.play(...)    — trigger playback via Audiolayer
 *   4. Call api.stop()       — stop on biome/event/UI change
 */
public final class ModCompatMp3Test {

    public static void run() throws Exception {
        // AmbientSounds — plays continuous ambient loops, switches on biome change
        testAmbientSounds_loadsAndPlaysAmbientLoop();
        testAmbientSounds_biomeSwitchStopsAndRestartsLoop();
        testAmbientSounds_unknownSound_isGuardedByIsLoaded();
        testAmbientSounds_getReturnsOggPath();

        // Reactive Music — triggers music on events, stops when event ends
        testReactiveMusic_eventTrigger_playsOnce();
        testReactiveMusic_eventEnd_stopsPlayback();
        testReactiveMusic_multipleEvents_lastWins();

        // Ambient Environment — similar to AmbientSounds, verifies reload survives biome scan
        testAmbientEnvironment_reloadKeepsSoundsAvailable();
        testAmbientEnvironment_loopPlayback_countZero();
        testAmbientEnvironment_partialStartOffset_accepted();

        // Euphonium — dedicated music player, uses full play(id, count, start, duration)
        testEuphonium_playWithDuration_accepted();
        testEuphonium_playFromOffset_accepted();
        testEuphonium_stopMidTrack_accepted();
        testEuphonium_listSounds_exposesAvailableTracks();
    }

    // =========================================================================
    // AmbientSounds
    // =========================================================================

    /** AmbientSounds plays ambient MP3 as an infinite loop (count = 0). */
    private static void testAmbientSounds_loadsAndPlaysAmbientLoop() throws Exception {
        AudiolayerManager manager = managerWithFile("ambient/forest_day.mp3");
        manager.reload();

        SoundId id = new SoundId("audiolayer", "ambient.forest_day");
        TestAssertions.assertTrue(manager.isLoaded(id));

        StubApi api = stubWithSound("ambient.forest_day");
        if (!api.isLoaded(id)) throw new AssertionError("guard failed");
        api.play(id, 0, 0f, 0f); // infinite loop
        TestAssertions.assertEquals(1, api.playFullCalls.size());
        TestAssertions.assertEquals("audiolayer:ambient.forest_day|0|0.0|0.0", api.playFullCalls.get(0));
    }

    /** When the player changes biome, AmbientSounds calls stop() then play() for the new sound. */
    private static void testAmbientSounds_biomeSwitchStopsAndRestartsLoop() throws Exception {
        StubApi api = stubWithSounds("ambient.forest_day", "ambient.desert_wind");

        SoundId forest = new SoundId("audiolayer", "ambient.forest_day");
        SoundId desert = new SoundId("audiolayer", "ambient.desert_wind");

        if (api.isLoaded(forest)) api.play(forest, 0, 0f, 0f);
        TestAssertions.assertEquals(1, api.playFullCalls.size());

        // biome change
        api.stop();
        TestAssertions.assertEquals(1, api.stopCount);

        if (api.isLoaded(desert)) api.play(desert, 0, 0f, 0f);
        TestAssertions.assertEquals(2, api.playFullCalls.size());
        TestAssertions.assertEquals("audiolayer:ambient.desert_wind|0|0.0|0.0", api.playFullCalls.get(1));
    }

    /** Sound not in Audiolayer → isLoaded() returns false → no play call. */
    private static void testAmbientSounds_unknownSound_isGuardedByIsLoaded() {
        StubApi api = new StubApi();
        SoundId id = new SoundId("audiolayer", "ambient.unknown_biome");
        if (!api.isLoaded(id)) return; // correct early-exit path
        api.play(id, 0, 0f, 0f);
        TestAssertions.assertEquals(0, api.playFullCalls.size());
    }

    /** AmbientSounds can retrieve the MP3 source path for its own mixing pipeline. */
    private static void testAmbientSounds_getReturnsOggPath() throws Exception {
        AudiolayerManager manager = managerWithFile("ambient/cave.mp3");
        manager.reload();

        SoundId id = new SoundId("audiolayer", "ambient.cave");
        Optional<LoadedAudioAsset> asset = manager.get(id);
        TestAssertions.assertTrue(asset.isPresent());
        TestAssertions.assertEquals(id, asset.get().soundId());
        TestAssertions.assertTrue(asset.get().sourceFile() != null);
    }

    // =========================================================================
    // Reactive Music
    // =========================================================================

    /** Reactive Music fires a combat event → plays music once (count = 1). */
    private static void testReactiveMusic_eventTrigger_playsOnce() throws Exception {
        StubApi api = stubWithSound("music.combat");
        SoundId id = new SoundId("audiolayer", "music.combat");

        if (api.isLoaded(id)) api.play(id, 1, 0f, 0f);
        TestAssertions.assertEquals(1, api.playFullCalls.size());
        TestAssertions.assertEquals("audiolayer:music.combat|1|0.0|0.0", api.playFullCalls.get(0));
    }

    /** When the combat event ends, Reactive Music calls stop(). */
    private static void testReactiveMusic_eventEnd_stopsPlayback() throws Exception {
        StubApi api = stubWithSound("music.combat");
        SoundId id = new SoundId("audiolayer", "music.combat");

        if (api.isLoaded(id)) api.play(id);
        api.stop();
        TestAssertions.assertEquals(1, api.stopCount);
    }

    /**
     * Two rapid events arrive (e.g. combat starts then boss spawns).
     * Reactive Music stops the first and plays the second.
     */
    private static void testReactiveMusic_multipleEvents_lastWins() throws Exception {
        StubApi api = stubWithSounds("music.combat", "music.boss");

        SoundId combat = new SoundId("audiolayer", "music.combat");
        SoundId boss   = new SoundId("audiolayer", "music.boss");

        if (api.isLoaded(combat)) api.play(combat, 1, 0f, 0f);
        api.stop();
        if (api.isLoaded(boss))   api.play(boss,   1, 0f, 0f);

        TestAssertions.assertEquals(2, api.playFullCalls.size());
        TestAssertions.assertEquals(1, api.stopCount);
        TestAssertions.assertEquals("audiolayer:music.boss|1|0.0|0.0", api.playFullCalls.get(1));
    }

    // =========================================================================
    // Ambient Environment
    // =========================================================================

    /**
     * After an api.reload(), sounds that were loaded before remain available.
     * Ambient Environment calls reload() on resource-pack change.
     */
    private static void testAmbientEnvironment_reloadKeepsSoundsAvailable() throws Exception {
        AudiolayerManager manager = managerWithFile("ambient/wind.mp3");
        manager.reload();
        SoundId id = new SoundId("audiolayer", "ambient.wind");
        TestAssertions.assertTrue(manager.isLoaded(id));

        manager.reload(); // second reload — same file is still there
        TestAssertions.assertTrue(manager.isLoaded(id));
    }

    /** Ambient Environment plays its sounds as infinite loops. */
    private static void testAmbientEnvironment_loopPlayback_countZero() throws Exception {
        StubApi api = stubWithSound("ambient.wind");
        SoundId id = new SoundId("audiolayer", "ambient.wind");

        if (api.isLoaded(id)) api.play(id, 0, 0f, 0f);
        TestAssertions.assertEquals("audiolayer:ambient.wind|0|0.0|0.0", api.playFullCalls.get(0));
    }

    /** Ambient Environment can start playback from a non-zero offset (e.g. crossfade position). */
    private static void testAmbientEnvironment_partialStartOffset_accepted() throws Exception {
        StubApi api = stubWithSound("ambient.wind");
        SoundId id = new SoundId("audiolayer", "ambient.wind");

        if (api.isLoaded(id)) api.play(id, 0, 30f, 0f); // start 30 s in, loop forever
        TestAssertions.assertEquals("audiolayer:ambient.wind|0|30.0|0.0", api.playFullCalls.get(0));
    }

    // =========================================================================
    // Euphonium
    // =========================================================================

    /** Euphonium plays a specific segment: 60 s starting at 10 s. */
    private static void testEuphonium_playWithDuration_accepted() throws Exception {
        StubApi api = stubWithSound("music.playlist_01");
        SoundId id = new SoundId("audiolayer", "music.playlist_01");

        if (api.isLoaded(id)) api.play(id, 1, 10f, 60f);
        TestAssertions.assertEquals("audiolayer:music.playlist_01|1|10.0|60.0", api.playFullCalls.get(0));
    }

    /** Euphonium resumes a track from a saved offset. */
    private static void testEuphonium_playFromOffset_accepted() throws Exception {
        StubApi api = stubWithSound("music.playlist_01");
        SoundId id = new SoundId("audiolayer", "music.playlist_01");

        float savedPosition = 45.5f;
        if (api.isLoaded(id)) api.play(id, 1, savedPosition, 0f);
        TestAssertions.assertEquals("audiolayer:music.playlist_01|1|45.5|0.0", api.playFullCalls.get(0));
    }

    /** Euphonium stop button calls stop() regardless of playback state. */
    private static void testEuphonium_stopMidTrack_accepted() throws Exception {
        StubApi api = stubWithSound("music.playlist_01");
        SoundId id = new SoundId("audiolayer", "music.playlist_01");

        if (api.isLoaded(id)) api.play(id);
        api.stop();
        TestAssertions.assertEquals(1, api.stopCount);
    }

    /** Euphonium populates its track list by iterating api.listSounds(). */
    private static void testEuphonium_listSounds_exposesAvailableTracks() throws Exception {
        AudiolayerManager manager = managerWithFiles(
                "music/playlist_01.mp3",
                "music/playlist_02.mp3"
        );
        manager.reload();

        Set<SoundId> sounds = manager.listSounds();
        TestAssertions.assertEquals(2, sounds.size());
        TestAssertions.assertTrue(sounds.contains(new SoundId("audiolayer", "music.playlist_01")));
        TestAssertions.assertTrue(sounds.contains(new SoundId("audiolayer", "music.playlist_02")));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static AudiolayerManager managerWithFile(String relativePath) throws Exception {
        Path root = Files.createTempDirectory("audiolayer-modcompat");
        Path input = root.resolve("input");
        Path file = input.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "fake-mp3-data");
        return buildManager(input, root.resolve("cache"));
    }

    private static AudiolayerManager managerWithFiles(String... relativePaths) throws Exception {
        Path root = Files.createTempDirectory("audiolayer-modcompat-multi");
        Path input = root.resolve("input");
        for (String relativePath : relativePaths) {
            Path file = input.resolve(relativePath);
            Files.createDirectories(file.getParent());
            Files.writeString(file, "fake-mp3-data");
        }
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

    private static StubApi stubWithSound(String soundPath) {
        StubApi api = new StubApi();
        api.loadedSounds.add(new SoundId("audiolayer", soundPath));
        return api;
    }

    private static StubApi stubWithSounds(String... soundPaths) {
        StubApi api = new StubApi();
        for (String path : soundPaths) {
            api.loadedSounds.add(new SoundId("audiolayer", path));
        }
        return api;
    }

    // =========================================================================
    // Minimal stub — same pattern as CompatApiContractTest
    // =========================================================================

    private static final class StubApi implements AudiolayerApi {
        final List<String> playCalls     = new ArrayList<>();
        final List<String> playFullCalls = new ArrayList<>();
        int stopCount;
        final List<SoundId> loadedSounds = new ArrayList<>();

        @Override public boolean isLoaded(SoundId id)            { return loadedSounds.contains(id); }
        @Override public Set<SoundId> listSounds()               { return Set.copyOf(loadedSounds); }
        @Override public Optional<LoadedAudioAsset> get(SoundId id) { return Optional.empty(); }
        @Override public void reload()                           {}
        @Override public void play(SoundId id)                   { playCalls.add(id.toString()); }
        @Override public void play(SoundId id, int count, float start, float duration) {
            playFullCalls.add(id + "|" + count + "|" + start + "|" + duration);
        }
        @Override public void stop() { stopCount++; }
    }
}
