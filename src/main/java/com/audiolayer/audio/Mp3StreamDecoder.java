package com.audiolayer.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * Streams an MP3 file incrementally, yielding PCM frames as 16-bit signed
 * little-endian stereo samples at the file's native sample rate.
 *
 * Usage:
 *   try (Mp3StreamDecoder dec = new Mp3StreamDecoder(path)) {
 *       short[] frame;
 *       while ((frame = dec.nextFrame()) != null) { ... }
 *   }
 */
public final class Mp3StreamDecoder implements AutoCloseable {
    // jlayer 1.0.1 uses ms_per_frame(), jlayer 1.0.3 (bundled by Etched) renamed it to msPerFrame()
    private static final Method MS_PER_FRAME = resolveMsPerFrame();

    private static Method resolveMsPerFrame() {
        try {
            return Header.class.getMethod("msPerFrame");
        } catch (NoSuchMethodException e) {
            try {
                return Header.class.getMethod("ms_per_frame");
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException("jlayer Header has neither msPerFrame() nor ms_per_frame()", ex);
            }
        }
    }

    private static float msPerFrame(Header h) {
        try {
            return (float) MS_PER_FRAME.invoke(h);
        } catch (Exception e) {
            return 0f;
        }
    }

    private final Bitstream bitstream;
    private final Decoder decoder;
    private final int channels;
    private final int sampleRate;

    public Mp3StreamDecoder(Path mp3) throws IOException, BitstreamException, DecoderException {
        FileInputStream fis = new FileInputStream(mp3.toFile());
        bitstream = new Bitstream(new BufferedInputStream(fis));
        decoder = new Decoder();

        // peek at first header to extract format info
        Header first = bitstream.readFrame();
        if (first == null) throw new IOException("Empty or invalid MP3: " + mp3);
        channels = first.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
        sampleRate = first.frequency();

        // decode and discard first frame so position is after it
        decoder.decodeFrame(first, bitstream);
        bitstream.closeFrame();
    }

    /** Returns the next decoded PCM frame, or null at end-of-stream. */
    public short[] nextFrame() throws BitstreamException {
        Header header = bitstream.readFrame();
        if (header == null) return null;
        try {
            SampleBuffer buf = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            short[] copy = new short[buf.getBufferLength()];
            System.arraycopy(buf.getBuffer(), 0, copy, 0, copy.length);
            return copy;
        } catch (Exception e) {
            return null;
        } finally {
            bitstream.closeFrame();
        }
    }

    public int channels() { return channels; }
    public int sampleRate() { return sampleRate; }

    @Override
    public void close() {
        try { bitstream.close(); } catch (BitstreamException ignored) {}
    }

    /**
     * Reads the full file to compute duration in seconds.
     * This is done once at reload time and cached — not on every play.
     */
    public static float readDurationSeconds(Path mp3) {
        float total = 0f;
        try (FileInputStream fis = new FileInputStream(mp3.toFile());
             var bis = new BufferedInputStream(fis)) {
            Bitstream bs = new Bitstream(bis);
            Header h;
            while ((h = bs.readFrame()) != null) {
                total += msPerFrame(h) / 1000f;
                bs.closeFrame();
            }
        } catch (Exception ignored) {}
        return total;
    }
}
