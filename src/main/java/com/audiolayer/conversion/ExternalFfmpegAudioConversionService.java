package com.audiolayer.conversion;

import com.audiolayer.audio.AudioSourceDescriptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ExternalFfmpegAudioConversionService implements AudioConversionService {
    private final String ffmpegCommand;

    public ExternalFfmpegAudioConversionService(String ffmpegCommand) {
        this.ffmpegCommand = ffmpegCommand;
    }

    @Override
    public ConversionResult convert(AudioSourceDescriptor source, Path outputFile) {
        try {
            Files.createDirectories(outputFile.getParent());
            Process process = new ProcessBuilder(
                    ffmpegCommand,
                    "-y",
                    "-i", source.absolutePath().toString(),
                    "-vn",
                    "-c:a", "libvorbis",
                    "-q:a", "4",
                    outputFile.toString()
            ).redirectErrorStream(true).start();

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return new ConversionResult(false, outputFile, output.trim());
            }
            return new ConversionResult(true, outputFile, output.trim());
        } catch (IOException e) {
            return new ConversionResult(false, outputFile, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ConversionResult(false, outputFile, "Interrupted while converting");
        }
    }
}
