package com.audiolayer.conversion;

import java.nio.file.Path;

public record ConversionResult(boolean success, Path outputFile, String message) {}
