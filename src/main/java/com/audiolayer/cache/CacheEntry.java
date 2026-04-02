package com.audiolayer.cache;

import java.nio.file.Path;
import java.time.Instant;

public record CacheEntry(
        String soundId,
        String sourceHash,
        String sourceRelativePath,
        Path cacheFile,
        Instant generatedAt
) {}
