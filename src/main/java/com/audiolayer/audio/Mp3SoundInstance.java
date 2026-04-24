package com.audiolayer.audio;

import com.mojang.logging.LogUtils;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Streams an MP3 file directly into an OpenAL source using buffer queuing.
 * Bypasses the MC SoundSystem entirely — no OGG conversion, no resource pack.
 */
public final class Mp3SoundInstance {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int BUFFER_COUNT = 4;
    private static final int BUFFER_SIZE_SAMPLES = 8192; // samples per channel per buffer

    private final Path sourceFile;
    private final int count;
    private final float startSeconds;
    private final float durationSeconds;

    private final int alSource;
    private final int[] alBuffers;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicBoolean released = new AtomicBoolean(false);
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "audiolayer-stream");
        t.setDaemon(true);
        return t;
    });

    private Future<?> streamTask;

    public Mp3SoundInstance(Path sourceFile, int count, float startSeconds, float durationSeconds) {
        this.sourceFile = sourceFile;
        this.count = count;
        this.startSeconds = startSeconds;
        this.durationSeconds = durationSeconds;

        alSource = AL10.alGenSources();
        alBuffers = new int[BUFFER_COUNT];
        AL10.alGenBuffers(alBuffers);
    }

    public void play() {
        streamTask = worker.submit(this::streamLoop);
    }

    public void stop() {
        stopped.set(true);
        if (streamTask != null) streamTask.cancel(true);
        releaseOpenAl();
    }

    public boolean isStopped() {
        if (stopped.get()) return true;
        if (released.get()) return true;
        int state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE);
        return state == AL10.AL_STOPPED && !isBuffersQueued();
    }

    private boolean isBuffersQueued() {
        return AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_QUEUED) > 0;
    }

    private void streamLoop() {
        StreamLoopController loop = new StreamLoopController(count, startSeconds, durationSeconds);

        try {
            Deque<Integer> freeBuffers = new ArrayDeque<>();
            for (int b : alBuffers) freeBuffers.add(b);

            Mp3StreamDecoder decoder = openDecoder(loop.startSeconds());
            if (decoder == null) return;

            int format = decoder.channels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            int sampleRate = decoder.sampleRate();

            // fill initial buffers
            for (int i = 0; i < BUFFER_COUNT && !freeBuffers.isEmpty(); i++) {
                int bufferId = freeBuffers.poll();
                short[] pcm = collectFrames(decoder, BUFFER_SIZE_SAMPLES * decoder.channels());
                if (pcm == null) break;
                uploadBuffer(bufferId, pcm, format, sampleRate);
                AL10.alSourceQueueBuffers(alSource, new int[]{bufferId});
                loop.advance((float) (pcm.length / decoder.channels()) / sampleRate);
            }

            AL10.alSourcePlay(alSource);

            while (!stopped.get() && !loop.isFinished()) {
                int processed = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_PROCESSED);
                while (processed-- > 0) {
                    int[] unqueued = new int[1];
                    AL10.alSourceUnqueueBuffers(alSource, unqueued);
                    freeBuffers.add(unqueued[0]);
                }

                while (!freeBuffers.isEmpty() && !loop.isFinished()) {
                    short[] pcm = collectFrames(decoder, BUFFER_SIZE_SAMPLES * decoder.channels());
                    StreamLoopController.Action action = pcm == null
                            ? loop.onEndOfFile()
                            : loop.advance((float) (pcm.length / decoder.channels()) / sampleRate);

                    if (action == StreamLoopController.Action.RESTART) {
                        decoder.close();
                        decoder = openDecoder(loop.startSeconds());
                        if (decoder == null) break;
                        if (pcm == null) {
                            pcm = collectFrames(decoder, BUFFER_SIZE_SAMPLES * decoder.channels());
                            if (pcm == null) break;
                            loop.advance((float) (pcm.length / decoder.channels()) / sampleRate);
                        }
                    } else if (action == StreamLoopController.Action.STOP) {
                        break;
                    }

                    if (pcm == null) break;
                    int bufferId = freeBuffers.poll();
                    uploadBuffer(bufferId, pcm, format, sampleRate);
                    AL10.alSourceQueueBuffers(alSource, new int[]{bufferId});
                }

                // restart if underrun
                int state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE);
                if (state != AL10.AL_PLAYING && isBuffersQueued()) {
                    AL10.alSourcePlay(alSource);
                }

                if (!isBuffersQueued() && loop.isFinished()) break;

                Thread.sleep(10);
            }

            decoder.close();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.error("Audiolayer stream error: {}", e.getMessage());
        } finally {
            stopped.set(true);
            releaseOpenAl();
        }
    }

    private void releaseOpenAl() {
        if (!released.compareAndSet(false, true)) return;
        AL10.alSourceStop(alSource);
        AL10.alSourcei(alSource, AL10.AL_BUFFER, 0);
        AL10.alDeleteBuffers(alBuffers);
        AL10.alDeleteSources(new int[]{alSource});
        worker.shutdown();
    }

    private Mp3StreamDecoder openDecoder(float seekSeconds) {
        try {
            Mp3StreamDecoder dec = new Mp3StreamDecoder(sourceFile);
            // coarse seek: skip frames until target time
            if (seekSeconds > 0) {
                float elapsed = 0f;
                short[] frame;
                while (elapsed < seekSeconds && (frame = dec.nextFrame()) != null) {
                    elapsed += (float) (frame.length / dec.channels()) / dec.sampleRate();
                }
            }
            return dec;
        } catch (Exception e) {
            LOGGER.error("Audiolayer: failed to open {}: {}", sourceFile, e.getMessage());
            return null;
        }
    }

    private short[] collectFrames(Mp3StreamDecoder decoder, int targetSamples) throws Exception {
        short[] accumulated = new short[targetSamples];
        int pos = 0;
        while (pos < targetSamples) {
            short[] frame = decoder.nextFrame();
            if (frame == null) {
                if (pos == 0) return null;
                short[] trimmed = new short[pos];
                System.arraycopy(accumulated, 0, trimmed, 0, pos);
                return trimmed;
            }
            int toCopy = Math.min(frame.length, targetSamples - pos);
            System.arraycopy(frame, 0, accumulated, pos, toCopy);
            pos += toCopy;
        }
        return accumulated;
    }

    private void uploadBuffer(int bufferId, short[] pcm, int format, int sampleRate) {
        ByteBuffer bb = ByteBuffer.allocateDirect(pcm.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short s : pcm) bb.putShort(s);
        bb.flip();
        AL10.alBufferData(bufferId, format, bb, sampleRate);
    }
}
