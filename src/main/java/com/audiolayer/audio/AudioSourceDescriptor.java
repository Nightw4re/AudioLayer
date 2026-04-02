package com.audiolayer.audio;

import com.audiolayer.api.SoundId;

import java.nio.file.Path;

public record AudioSourceDescriptor(
        Path absolutePath,
        String relativePath,
        String extension,
        long fileSize,
        String contentHash,
        String category,
        String soundPath,
        SoundId soundId
) {}
