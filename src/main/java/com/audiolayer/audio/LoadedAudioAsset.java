package com.audiolayer.audio;

import com.audiolayer.api.SoundId;

import java.nio.file.Path;

public record LoadedAudioAsset(
        SoundId soundId,
        Path sourceFile,
        Path cacheFile,
        String sourceHash,
        float durationSeconds
) {}
