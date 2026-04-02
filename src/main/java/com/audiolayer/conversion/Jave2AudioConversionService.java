package com.audiolayer.conversion;

import com.audiolayer.audio.AudioSourceDescriptor;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Jave2AudioConversionService implements AudioConversionService {

    @Override
    public ConversionResult convert(AudioSourceDescriptor source, Path outputFile) {
        try {
            Files.createDirectories(outputFile.getParent());

            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libvorbis");
            audio.setBitRate(128000);
            audio.setChannels(2);
            audio.setSamplingRate(44100);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("ogg");
            attrs.setAudioAttributes(audio);

            new Encoder().encode(
                    new MultimediaObject(source.absolutePath().toFile()),
                    outputFile.toFile(),
                    attrs
            );
            return new ConversionResult(true, outputFile, "");
        } catch (EncoderException e) {
            return new ConversionResult(false, outputFile, e.getMessage());
        } catch (Exception e) {
            return new ConversionResult(false, outputFile, e.getMessage());
        }
    }

    public static float readDurationSeconds(java.nio.file.Path file) {
        try {
            long ms = new ws.schild.jave.MultimediaObject(file.toFile()).getInfo().getDuration();
            return ms / 1000f;
        } catch (Exception e) {
            return 0f;
        }
    }
}
