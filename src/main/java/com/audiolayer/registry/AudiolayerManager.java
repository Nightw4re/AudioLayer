package com.audiolayer.registry;

import com.audiolayer.api.AudiolayerService;
import com.audiolayer.api.ReloadSummary;
import com.audiolayer.api.SoundId;
import com.audiolayer.audio.AudioSourceDescriptor;
import com.audiolayer.audio.InputAudioScanner;
import com.audiolayer.audio.LoadedAudioAsset;
import com.audiolayer.cache.CacheEntry;
import com.audiolayer.cache.CacheIndex;
import com.audiolayer.cache.CacheIndexRepository;
import com.audiolayer.config.AudiolayerConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

public final class AudiolayerManager implements AudiolayerService {
    private static final Logger LOGGER = Logger.getLogger(AudiolayerManager.class.getName());
    private final AudiolayerConfig config;
    private final InputAudioScanner scanner;
    private final CacheIndexRepository cacheIndexRepository;
    private final Function<Path, Float> durationReader;
    private final Map<SoundId, LoadedAudioAsset> loaded = new LinkedHashMap<>();

    public AudiolayerManager(
            AudiolayerConfig config,
            InputAudioScanner scanner,
            CacheIndexRepository cacheIndexRepository,
            Function<Path, Float> durationReader
    ) {
        this.config = config;
        this.scanner = scanner;
        this.cacheIndexRepository = cacheIndexRepository;
        this.durationReader = durationReader;
    }

    @Override
    public boolean isLoaded(SoundId id) {
        return loaded.containsKey(id);
    }

    @Override
    public Set<SoundId> listSounds() {
        return Set.copyOf(loaded.keySet());
    }

    @Override
    public Optional<LoadedAudioAsset> get(SoundId id) {
        return Optional.ofNullable(loaded.get(id));
    }

    @Override
    public ReloadSummary reload() {
        try {
            var descriptors = scanner.scan(config.inputDirectory());
            var index = cacheIndexRepository.load();
            loaded.clear();

            Map<String, CacheEntry> entriesByHash = new LinkedHashMap<>();
            for (CacheEntry e : index.entries()) {
                entriesByHash.put(e.sourceHash(), e);
            }

            int reused = 0;

            for (AudioSourceDescriptor descriptor : descriptors) {
                CacheEntry existing = entriesByHash.get(descriptor.contentHash());
                float duration;
                if (existing != null) {
                    duration = existing.durationSeconds();
                    reused++;
                } else {
                    duration = durationReader.apply(descriptor.absolutePath());
                }
                loaded.put(descriptor.soundId(), new LoadedAudioAsset(
                        descriptor.soundId(),
                        descriptor.absolutePath(),
                        descriptor.contentHash(),
                        duration
                ));
            }

            cacheIndexRepository.save(new CacheIndex(descriptors.stream()
                    .map(d -> {
                        float dur = loaded.containsKey(d.soundId())
                                ? loaded.get(d.soundId()).durationSeconds()
                                : 0f;
                        return new CacheEntry(
                                d.soundId().toString(),
                                d.contentHash(),
                                d.relativePath(),
                                dur,
                                Instant.now()
                        );
                    })
                    .toList()));

            return new ReloadSummary(descriptors.size(), loaded.size(), reused, 0);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to reload audiolayer", e);
        }
    }
}
