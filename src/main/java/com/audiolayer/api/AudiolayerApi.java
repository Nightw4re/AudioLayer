package com.audiolayer.api;

import java.util.Optional;
import java.util.Set;

/**
 * Public API for Audiolayer. Other mods can access this via {@link AudiolayerProvider#get()}.
 *
 * <p>All play methods are client-side only — do not call from server-side code.
 */
public interface AudiolayerApi {
    /** Returns true if the sound identified by {@code id} is loaded and ready to play. */
    boolean isLoaded(SoundId id);

    /** Returns all currently loaded sound IDs. */
    Set<SoundId> listSounds();

    /**
     * Returns the loaded asset for the given sound ID, or empty if not loaded.
     * The asset exposes the path to the converted OGG cache file, which other mods
     * can use for their own playback or processing.
     */
    java.util.Optional<com.audiolayer.audio.LoadedAudioAsset> get(SoundId id);

    /** Rescans the input directory and rebuilds the runtime resource pack. */
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

    /** Stops the currently playing Audiolayer sound. */
    void stop();
}
