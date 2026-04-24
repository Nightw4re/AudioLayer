package com.audiolayer.kubejs;

import com.audiolayer.api.AudiolayerApi;
import com.audiolayer.api.AudiolayerProvider;
import com.audiolayer.api.SoundId;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * JavaScript-friendly wrapper around {@link AudiolayerApi}.
 *
 * <p>Exposed to KubeJS scripts as the global {@code Audiolayer} binding.
 * All methods are no-ops (or return safe defaults) when the API is unavailable.
 *
 * <p>Sound IDs use the {@code namespace:path} format, e.g. {@code "audiolayer:music.theme"}.
 */
public final class AudiolayerJSWrapper {

    private final Supplier<Optional<AudiolayerApi>> apiSupplier;

    /** Production constructor - resolves through {@link AudiolayerProvider}. */
    public AudiolayerJSWrapper() {
        this(AudiolayerProvider::get);
    }

    /** Testing constructor - injects a custom API supplier. */
    AudiolayerJSWrapper(Supplier<Optional<AudiolayerApi>> apiSupplier) {
        this.apiSupplier = apiSupplier;
    }

    /** Plays a sound once from the beginning. */
    public void play(String soundId) {
        api().ifPresent(a -> a.play(parseSoundId(soundId)));
    }

    /**
     * Plays a sound with full control.
     *
     * @param soundId  sound in {@code namespace:path} format
     * @param count    repetitions; 0 = infinite loop
     * @param start    start position in seconds; 0 = from beginning
     * @param duration per-repetition duration in seconds; 0 = until end of file
     */
    public void play(String soundId, int count, double start, double duration) {
        api().ifPresent(a -> a.play(parseSoundId(soundId), count, (float) start, (float) duration));
    }

    /**
     * Plays a sound with mixer options.
     *
     * @param soundId  sound in {@code namespace:path} format
     * @param count    repetitions; 0 = infinite loop
     * @param start    start position in seconds; 0 = from beginning
     * @param duration per-repetition duration in seconds; 0 = until end of file
     * @param volume   gain; 1 = normal volume
     * @param pitch    pitch; 1 = normal pitch
     * @param category logical category such as music, ambient, or ui
     */
    public void play(String soundId, int count, double start, double duration, double volume, double pitch, String category) {
        api().ifPresent(a -> a.play(
                parseSoundId(soundId),
                count,
                (float) start,
                (float) duration,
                (float) volume,
                (float) pitch,
                category
        ));
    }

    /** Stops the currently playing Audiolayer sound. */
    public void stop() {
        api().ifPresent(AudiolayerApi::stop);
    }

    /** Stops the sound currently playing in the given category/channel. */
    public void stop(String category) {
        api().ifPresent(a -> a.stop(category));
    }

    /** Returns {@code true} if the given sound is loaded and ready to play. */
    public boolean isLoaded(String soundId) {
        return api().map(a -> a.isLoaded(parseSoundId(soundId))).orElse(false);
    }

    /** Returns all currently loaded sound IDs sorted, as {@code namespace:path} strings. */
    public List<String> listSounds() {
        return api()
                .map(a -> a.listSounds().stream().map(SoundId::toString).sorted().toList())
                .orElse(Collections.emptyList());
    }

    /** Rescans the input directory and updates the duration cache. */
    public void reload() {
        api().ifPresent(AudiolayerApi::reload);
    }

    // ---- internal ----

    private Optional<AudiolayerApi> api() {
        return apiSupplier.get();
    }

    static SoundId parseSoundId(String raw) {
        int colon = raw.indexOf(':');
        if (colon < 1 || colon == raw.length() - 1) {
            throw new IllegalArgumentException(
                    "Invalid sound ID '" + raw + "': expected 'namespace:path'");
        }
        return new SoundId(raw.substring(0, colon), raw.substring(colon + 1));
    }
}
