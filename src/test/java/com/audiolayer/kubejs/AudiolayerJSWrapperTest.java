package com.audiolayer.kubejs;

import com.audiolayer.api.AudiolayerApi;
import com.audiolayer.audio.LoadedAudioAsset;
import com.audiolayer.api.SoundId;
import com.audiolayer.testsupport.TestAssertions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class AudiolayerJSWrapperTest {

    public static void run() {
        testParseSoundId_valid();
        testParseSoundId_missingColon();
        testParseSoundId_emptyNamespace();
        testParseSoundId_emptyPath();

        testPlay_simple_dispatchesToApi();
        testPlay_full_dispatchesToApi();
        testStop_dispatchesToApi();
        testIsLoaded_returnsApiValue();
        testListSounds_returnsSortedStrings();
        testReload_dispatchesToApi();

        testPlay_noApiRegistered_isNoOp();
        testStop_noApiRegistered_isNoOp();
        testIsLoaded_noApiRegistered_returnsFalse();
        testListSounds_noApiRegistered_returnsEmpty();
    }

    // ---- parseSoundId ----

    private static void testParseSoundId_valid() {
        SoundId id = AudiolayerJSWrapper.parseSoundId("audiolayer:music.theme");
        TestAssertions.assertEquals("audiolayer", id.namespace());
        TestAssertions.assertEquals("music.theme", id.path());
    }

    private static void testParseSoundId_missingColon() {
        TestAssertions.assertThrows(IllegalArgumentException.class,
                () -> AudiolayerJSWrapper.parseSoundId("audiolayermusic"));
    }

    private static void testParseSoundId_emptyNamespace() {
        TestAssertions.assertThrows(IllegalArgumentException.class,
                () -> AudiolayerJSWrapper.parseSoundId(":music.theme"));
    }

    private static void testParseSoundId_emptyPath() {
        TestAssertions.assertThrows(IllegalArgumentException.class,
                () -> AudiolayerJSWrapper.parseSoundId("audiolayer:"));
    }

    // ---- with API registered ----

    private static void testPlay_simple_dispatchesToApi() {
        StubApi stub = new StubApi();
        AudiolayerJSWrapper wrapper = new AudiolayerJSWrapper(() -> Optional.of(stub));
        wrapper.play("audiolayer:music.theme");
        TestAssertions.assertEquals(1, stub.playCalls.size());
        TestAssertions.assertEquals("audiolayer:music.theme", stub.playCalls.get(0));
    }

    private static void testPlay_full_dispatchesToApi() {
        StubApi stub = new StubApi();
        AudiolayerJSWrapper wrapper = new AudiolayerJSWrapper(() -> Optional.of(stub));
        wrapper.play("audiolayer:music.theme", 3, 1.5, 10.0);
        TestAssertions.assertEquals(1, stub.playFullCalls.size());
        String call = stub.playFullCalls.get(0);
        TestAssertions.assertEquals("audiolayer:music.theme|3|1.5|10.0", call);
    }

    private static void testStop_dispatchesToApi() {
        StubApi stub = new StubApi();
        AudiolayerJSWrapper wrapper = new AudiolayerJSWrapper(() -> Optional.of(stub));
        wrapper.stop();
        TestAssertions.assertEquals(1, stub.stopCount);
    }

    private static void testIsLoaded_returnsApiValue() {
        StubApi stub = new StubApi();
        stub.loadedId = new SoundId("audiolayer", "music.theme");
        AudiolayerJSWrapper wrapper = new AudiolayerJSWrapper(() -> Optional.of(stub));
        TestAssertions.assertTrue(wrapper.isLoaded("audiolayer:music.theme"));
        TestAssertions.assertTrue(!wrapper.isLoaded("audiolayer:other"));
    }

    private static void testListSounds_returnsSortedStrings() {
        StubApi stub = new StubApi();
        stub.sounds = Set.of(
                new SoundId("audiolayer", "ui.confirm"),
                new SoundId("audiolayer", "ambience.cave"),
                new SoundId("audiolayer", "music.theme")
        );
        AudiolayerJSWrapper wrapper = new AudiolayerJSWrapper(() -> Optional.of(stub));
        List<String> result = wrapper.listSounds();
        TestAssertions.assertEquals(3, result.size());
        TestAssertions.assertEquals("audiolayer:ambience.cave", result.get(0));
        TestAssertions.assertEquals("audiolayer:music.theme", result.get(1));
        TestAssertions.assertEquals("audiolayer:ui.confirm", result.get(2));
    }

    private static void testReload_dispatchesToApi() {
        StubApi stub = new StubApi();
        AudiolayerJSWrapper wrapper = new AudiolayerJSWrapper(() -> Optional.of(stub));
        wrapper.reload();
        TestAssertions.assertEquals(1, stub.reloadCount);
    }

    // ---- without API registered ----

    private static void testPlay_noApiRegistered_isNoOp() {
        AudiolayerJSWrapper wrapper = new AudiolayerJSWrapper(Optional::empty);
        wrapper.play("audiolayer:music.theme"); // must not throw
    }

    private static void testStop_noApiRegistered_isNoOp() {
        AudiolayerJSWrapper wrapper = new AudiolayerJSWrapper(Optional::empty);
        wrapper.stop(); // must not throw
    }

    private static void testIsLoaded_noApiRegistered_returnsFalse() {
        AudiolayerJSWrapper wrapper = new AudiolayerJSWrapper(Optional::empty);
        TestAssertions.assertTrue(!wrapper.isLoaded("audiolayer:music.theme"));
    }

    private static void testListSounds_noApiRegistered_returnsEmpty() {
        AudiolayerJSWrapper wrapper = new AudiolayerJSWrapper(Optional::empty);
        TestAssertions.assertEquals(0, wrapper.listSounds().size());
    }

    // ---- stub ----

    private static final class StubApi implements AudiolayerApi {
        final List<String> playCalls = new ArrayList<>();
        final List<String> playFullCalls = new ArrayList<>();
        int stopCount;
        int reloadCount;
        SoundId loadedId;
        Set<SoundId> sounds = Set.of();

        @Override
        public boolean isLoaded(SoundId id) {
            return id.equals(loadedId);
        }

        @Override
        public Set<SoundId> listSounds() {
            return sounds;
        }

        @Override
        public Optional<LoadedAudioAsset> get(SoundId id) {
            return Optional.empty();
        }

        @Override
        public void reload() {
            reloadCount++;
        }

        @Override
        public void play(SoundId id) {
            playCalls.add(id.toString());
        }

        @Override
        public void play(SoundId id, int count, float startSeconds, float durationSeconds) {
            playFullCalls.add(id + "|" + count + "|" + startSeconds + "|" + durationSeconds);
        }

        @Override
        public void stop() {
            stopCount++;
        }
    }
}
