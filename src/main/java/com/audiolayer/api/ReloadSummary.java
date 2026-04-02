package com.audiolayer.api;

public record ReloadSummary(int scannedFiles, int loadedAssets, int reusedAssets, int failedFiles) {}
