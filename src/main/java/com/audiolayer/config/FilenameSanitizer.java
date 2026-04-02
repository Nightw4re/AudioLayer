package com.audiolayer.config;

import java.text.Normalizer;
import java.util.Locale;

public final class FilenameSanitizer {
    public String sanitizeSegment(String raw) {
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Invalid audio name segment: " + raw);
        }
        return normalized;
    }
}
