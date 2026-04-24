package com.audiolayer.api;

import java.util.Set;

/**
 * Public API for Audiolayer. Other mods can access this via {@link AudiolayerProvider#get()}.
 *
 * <p>All play methods are client-side only - do not call from server-side code.
 */
public interface AudiolayerApi {
    /** Returns true if the sound identified by {@code id} is loaded and ready to play. */
    boolean isLoaded(SoundId id);

    /** Returns all currently loaded sound IDs. */
    Set<SoundId> listSounds();

    /**
     * Returns the loaded asset for the given sound ID, or empty if not loaded.
     * The asset exposes the original MP3 path and metadata.
     */
    java.util.Optional<com.audiolayer.audio.LoadedAudioAsset> get(SoundId id);

    /** Rescans the input directory and updates the duration cache. */
    void reload();

    /**
     * Plays a sound once from the beginning.
     *
     * @param id sound to play
     */
    void play(SoundId id);

    /**
     * Plays a sound with full control.
     *
     * @param id              sound to play
     * @param count           number of repetitions; 0 = infinite loop
     * @param startSeconds    position in the file to start from; 0 = from the beginning
     * @param durationSeconds duration of each repetition in seconds; 0 = until end of file
     */
    void play(SoundId id, int count, float startSeconds, float durationSeconds);

    /**
     * Plays a sound with mixer options for integrations that need finer control.
     *
     * @param id              sound to play
     * @param count           number of repetitions; 0 = infinite loop
     * @param startSeconds    position in the file to start from; 0 = from the beginning
     * @param durationSeconds duration of each repetition in seconds; 0 = until end of file
     * @param volume          OpenAL gain; 1 = normal volume
     * @param pitch           OpenAL pitch; 1 = normal pitch
     * @param category        logical category for callers, e.g. music, ambient, ui
     */
    default void play(
            SoundId id,
            int count,
            float startSeconds,
            float durationSeconds,
            float volume,
            float pitch,
            String category
    ) {
        play(id, count, startSeconds, durationSeconds);
    }

    /** Stops the currently playing Audiolayer sound. */
    void stop();

    /** Stops the sound currently playing in the given category/channel. */
    default void stop(String category) {
        stop();
    }
}
