package com.audiolayer.cache;

import java.time.Instant;

public record CacheEntry(
        String soundId,
        String sourceHash,
        String sourceRelativePath,
        float durationSeconds,
        Instant generatedAt
) {}
