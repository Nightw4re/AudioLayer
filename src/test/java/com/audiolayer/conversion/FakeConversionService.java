package com.audiolayer.conversion;

import com.audiolayer.audio.AudioSourceDescriptor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

public final class FakeConversionService implements AudioConversionService {
    private final boolean success;
    private int callCount = 0;

    public FakeConversionService(boolean success) {
        this.success = success;
    }

    public int callCount() {
        return callCount;
    }

    @Override
    public ConversionResult convert(AudioSourceDescriptor source, Path outputFile) {
        callCount++;
        try {
            if (success) {
                Files.createDirectories(outputFile.getParent());
                Files.writeString(outputFile, "fake-ogg");
            }
        } catch (IOException e) {
            return new ConversionResult(false, outputFile, e.getMessage());
        }
        return new ConversionResult(success, outputFile, success ? "ok" : "failed");
    }
}
