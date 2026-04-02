package com.audiolayer.conversion;

import com.audiolayer.audio.AudioSourceDescriptor;

import java.nio.file.Path;

public interface AudioConversionService {
    ConversionResult convert(AudioSourceDescriptor source, Path outputFile);
}
