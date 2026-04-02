package com.audiolayer.cache;

import com.audiolayer.testsupport.TestAssertions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public final class JsonCacheIndexRepositoryTest {
    public static void run() throws Exception {
        Path file = Files.createTempFile("audiolayer-index", ".txt");
        JsonCacheIndexRepository repo = new JsonCacheIndexRepository(file);
        CacheIndex index = new CacheIndex(java.util.List.of(new CacheEntry(
                "audiolayer:music.atlantis_intro",
                "hash",
                "music/atlantis_intro.mp3",
                120f,
                Instant.parse("2026-03-29T00:00:00Z")
        )));

        repo.save(index);
        CacheIndex loaded = repo.load();

        TestAssertions.assertEquals(1, loaded.entries().size());
        TestAssertions.assertEquals(index.entries().get(0).soundId(), loaded.entries().get(0).soundId());
    }
}
