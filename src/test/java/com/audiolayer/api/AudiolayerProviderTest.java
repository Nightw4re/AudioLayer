package com.audiolayer.api;

import com.audiolayer.audio.LoadedAudioAsset;
import com.audiolayer.testsupport.TestAssertions;

import java.util.Optional;

public final class AudiolayerProviderTest {
    public static void run() {
        AudiolayerApi api = new AudiolayerApi() {
            @Override
            public boolean isLoaded(SoundId id) {
                return false;
            }

            @Override
            public java.util.Set<SoundId> listSounds() {
                return java.util.Set.of();
            }

            @Override
            public java.util.Optional<LoadedAudioAsset> get(SoundId id) {
                return java.util.Optional.empty();
            }

            @Override
            public void reload() {
            }

            @Override
            public void play(SoundId id) {
            }

            @Override
            public void play(SoundId id, int count, float startSeconds, float durationSeconds) {
            }

            @Override
            public void stop() {
            }
        };

        AudiolayerProvider.register(api);
        TestAssertions.assertEquals(Optional.of(api), AudiolayerProvider.get());
        TestAssertions.assertThrows(IllegalStateException.class, () -> AudiolayerProvider.register(api));
    }
}
