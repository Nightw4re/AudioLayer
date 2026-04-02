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
import com.audiolayer.conversion.AudioConversionService;
import com.audiolayer.conversion.ConversionResult;
import com.audiolayer.resource.RuntimeResourcePackWriter;

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
    private final AudioConversionService conversionService;
    private final AudioRegistryService registry;
    private final RuntimeResourcePackWriter resourcePackWriter;
    private final Function<Path, Float> durationReader;
    private final Map<SoundId, LoadedAudioAsset> loaded = new LinkedHashMap<>();

    public AudiolayerManager(
            AudiolayerConfig config,
            InputAudioScanner scanner,
            CacheIndexRepository cacheIndexRepository,
            AudioRegistryService registry,
            AudioConversionService conversionService,
            RuntimeResourcePackWriter resourcePackWriter,
            Function<Path, Float> durationReader
    ) {
        this.config = config;
        this.scanner = scanner;
        this.cacheIndexRepository = cacheIndexRepository;
        this.conversionService = conversionService;
        this.registry = registry;
        this.resourcePackWriter = resourcePackWriter;
        this.durationReader = durationReader;
    }

    public AudiolayerManager(
            AudiolayerConfig config,
            InputAudioScanner scanner,
            CacheIndexRepository cacheIndexRepository,
            AudioRegistryService registry,
            AudioConversionService conversionService,
            RuntimeResourcePackWriter resourcePackWriter
    ) {
        this(config, scanner, cacheIndexRepository, registry, conversionService, resourcePackWriter, path -> 0f);
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
            int reused = 0;
            int failed = 0;
            Map<String, CacheEntry> entriesByHash = index.entries().stream().collect(java.util.stream.Collectors.toMap(
                    CacheEntry::sourceHash,
                    entry -> entry,
                    (a, b) -> a,
                    LinkedHashMap::new
            ));
            for (AudioSourceDescriptor descriptor : descriptors) {
                CacheEntry existing = entriesByHash.get(descriptor.contentHash());
                Path cacheFile = existing != null ? existing.cacheFile() : config.cacheDirectory().resolve(descriptor.contentHash() + ".ogg");
                if (existing != null && java.nio.file.Files.exists(cacheFile)) {
                    reused++;
                } else {
                    ConversionResult conversion = conversionService.convert(descriptor, cacheFile);
                    if (!conversion.success()) {
                        LOGGER.severe("Conversion failed for " + descriptor.absolutePath() + ": " + conversion.message());
                        failed++;
                        continue;
                    }
                }
                float duration = durationReader.apply(cacheFile);
                loaded.put(descriptor.soundId(), new LoadedAudioAsset(
                        descriptor.soundId(),
                        descriptor.absolutePath(),
                        cacheFile,
                        descriptor.contentHash(),
                        duration
                ));
            }
            registry.rebuild(descriptors, config.cacheDirectory());
            cacheIndexRepository.save(new CacheIndex(descriptors.stream()
                    .map(d -> new CacheEntry(
                            d.soundId().toString(),
                            d.contentHash(),
                            d.relativePath(),
                            config.cacheDirectory().resolve(d.contentHash() + ".ogg"),
                            Instant.now()
                    ))
                    .toList()));
            resourcePackWriter.write(loaded.values());
            return new ReloadSummary(descriptors.size(), loaded.size(), reused, failed);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to reload audio bridge", e);
        }
    }

}
