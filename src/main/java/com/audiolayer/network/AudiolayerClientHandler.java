package com.audiolayer.network;

import com.audiolayer.api.AudiolayerApi;
import com.audiolayer.api.AudiolayerProvider;

public final class AudiolayerClientHandler {
    private AudiolayerClientHandler() {}

    public static void onPlay(AudiolayerPlayPacket pkt) {
        AudiolayerProvider.get().ifPresent(api ->
                api.play(
                        pkt.soundId(),
                        pkt.count(),
                        pkt.startSeconds(),
                        pkt.durationSeconds(),
                        pkt.volume(),
                        pkt.pitch(),
                        pkt.category()
                ));
    }

    public static void onStop() {
        onStop(new AudiolayerStopPacket());
    }

    public static void onStop(AudiolayerStopPacket pkt) {
        AudiolayerProvider.get().ifPresent(api -> {
            if (pkt.category() == null || pkt.category().isBlank()) {
                api.stop();
            } else {
                api.stop(pkt.category());
            }
        });
    }
}
