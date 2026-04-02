package com.audiolayer.config;

import java.nio.file.Path;

public record AudiolayerConfig(
        Path inputDirectory,
        Path cacheDirectory,
        boolean autoReloadEnabled
) {}
