package com.audiolayer.network;

import com.audiolayer.api.AudiolayerApi;
import com.audiolayer.api.AudiolayerProvider;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class AudiolayerClientHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private AudiolayerClientHandler() {}

    public static void onPlay(AudiolayerPlayPacket pkt) {
        var api = AudiolayerProvider.get();
        if (api.isEmpty()) {
            LOGGER.warn("Audiolayer client API is unavailable; cannot play {}", pkt.soundId());
            return;
        }
        if (!api.get().isLoaded(pkt.soundId())) {
            LOGGER.warn("Audiolayer client is missing sound {}; reload or sync config/audiolayer/input", pkt.soundId());
            return;
        }
        api.get().play(
                pkt.soundId(),
                pkt.count(),
                pkt.startSeconds(),
                pkt.durationSeconds(),
                pkt.volume(),
                pkt.pitch(),
                pkt.category()
        );
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
