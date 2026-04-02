package com.audiolayer.api;

import java.util.Optional;

/**
 * Static access point for the Audiolayer API.
 *
 * <p>Usage from another mod:
 * <pre>{@code
 * AudiolayerProvider.get().ifPresent(api -> {
 *     if (api.isLoaded(new SoundId("audiolayer", "music.track"))) {
 *         api.play(new SoundId("audiolayer", "music.track"));
 *     }
 * });
 * }</pre>
 *
 * <p>The instance is available after Audiolayer's mod constructor runs.
 * It is safe to store the {@link AudiolayerApi} reference for later use.
 */
public final class AudiolayerProvider {
    private static AudiolayerApi instance;

    private AudiolayerProvider() {}

    /** Returns the Audiolayer API, or empty if Audiolayer is not loaded. */
    public static Optional<AudiolayerApi> get() {
        return Optional.ofNullable(instance);
    }

    /** Called by Audiolayer internally during mod initialization. */
    public static void register(AudiolayerApi api) {
        if (instance != null) throw new IllegalStateException("AudiolayerApi already registered");
        instance = api;
    }
}
