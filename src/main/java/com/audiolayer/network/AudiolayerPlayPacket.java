package com.audiolayer.network;

import com.audiolayer.AudiolayerMod;
import com.audiolayer.api.SoundId;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AudiolayerPlayPacket(
        SoundId soundId,
        int count,
        float startSeconds,
        float durationSeconds,
        float volume,
        float pitch,
        String category
)
        implements CustomPacketPayload {

    public static final Type<AudiolayerPlayPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AudiolayerMod.MODID, "play")
    );

    public AudiolayerPlayPacket(SoundId soundId, int count, float startSeconds, float durationSeconds) {
        this(soundId, count, startSeconds, durationSeconds, 1f, 1f, "master");
    }

    public static final StreamCodec<FriendlyByteBuf, AudiolayerPlayPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUtf(pkt.soundId().toString());
                buf.writeInt(pkt.count());
                buf.writeFloat(pkt.startSeconds());
                buf.writeFloat(pkt.durationSeconds());
                buf.writeFloat(pkt.volume());
                buf.writeFloat(pkt.pitch());
                buf.writeUtf(pkt.category());
            },
            buf -> {
                String raw = buf.readUtf();
                int sep = raw.indexOf(':');
                SoundId id = sep > 0
                        ? new SoundId(raw.substring(0, sep), raw.substring(sep + 1))
                        : new SoundId("audiolayer", raw);
                return new AudiolayerPlayPacket(
                        id,
                        buf.readInt(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readUtf()
                );
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
