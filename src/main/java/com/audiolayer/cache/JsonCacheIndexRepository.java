package com.audiolayer.cache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class JsonCacheIndexRepository implements CacheIndexRepository {
    private final Path indexFile;

    public JsonCacheIndexRepository(Path indexFile) {
        this.indexFile = indexFile;
    }

    @Override
    public CacheIndex load() throws IOException {
        if (!Files.exists(indexFile)) {
            return new CacheIndex(List.of());
        }
        String content = Files.readString(indexFile);
        if (content.isBlank()) {
            return new CacheIndex(List.of());
        }
        List<CacheEntry> entries = new ArrayList<>();
        for (String line : content.strip().split("\n")) {
            String[] parts = line.split("\\|", -1);
            if (parts.length == 5) {
                entries.add(new CacheEntry(parts[0], parts[1], parts[2], Path.of(parts[3]), Instant.parse(parts[4])));
            }
        }
        return new CacheIndex(entries);
    }

    @Override
    public void save(CacheIndex index) throws IOException {
        Files.createDirectories(indexFile.getParent());
        StringBuilder sb = new StringBuilder();
        for (CacheEntry entry : index.entries()) {
            sb.append(entry.soundId()).append("|")
                    .append(entry.sourceHash()).append("|")
                    .append(entry.sourceRelativePath()).append("|")
                    .append(entry.cacheFile()).append("|")
                    .append(entry.generatedAt()).append("\n");
        }
        Files.writeString(indexFile, sb.toString(), StandardCharsets.UTF_8);
    }
}
