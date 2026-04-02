package com.audiolayer.config;

import com.audiolayer.api.SoundId;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SoundIdMapper {
    private final FilenameSanitizer sanitizer;

    public SoundIdMapper(FilenameSanitizer sanitizer) {
        this.sanitizer = sanitizer;
    }

    public SoundId map(Path relativeInputPath) {
        Path withoutExtension = stripExtension(relativeInputPath);
        List<String> parts = new ArrayList<>();
        for (Path part : withoutExtension) {
            parts.add(sanitizer.sanitizeSegment(part.toString()));
        }
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Path must contain at least one segment");
        }
        String soundPath = String.join(".", parts);
        return new SoundId("audiolayer", soundPath);
    }

    private Path stripExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        return path.getParent() == null ? Path.of(base) : path.getParent().resolve(base);
    }
}
