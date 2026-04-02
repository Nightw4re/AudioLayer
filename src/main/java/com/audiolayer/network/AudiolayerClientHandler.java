package com.audiolayer.network;

import com.audiolayer.api.AudiolayerApi;
import com.audiolayer.api.AudiolayerProvider;

public final class AudiolayerClientHandler {
    private AudiolayerClientHandler() {}

    public static void onPlay(AudiolayerPlayPacket pkt) {
        AudiolayerProvider.get().ifPresent(api ->
                api.play(pkt.soundId(), pkt.count(), pkt.startSeconds(), pkt.durationSeconds()));
    }

    public static void onStop() {
        AudiolayerProvider.get().ifPresent(AudiolayerApi::stop);
    }
}
