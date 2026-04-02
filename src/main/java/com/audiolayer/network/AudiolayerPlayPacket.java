package com.audiolayer.network;

import com.audiolayer.AudiolayerMod;
import com.audiolayer.api.SoundId;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AudiolayerPlayPacket(SoundId soundId, int count, float startSeconds, float durationSeconds)
        implements CustomPacketPayload {

    public static final Type<AudiolayerPlayPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AudiolayerMod.MODID, "play")
    );

    public static final StreamCodec<FriendlyByteBuf, AudiolayerPlayPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUtf(pkt.soundId().toString());
                buf.writeInt(pkt.count());
                buf.writeFloat(pkt.startSeconds());
                buf.writeFloat(pkt.durationSeconds());
            },
            buf -> {
                String raw = buf.readUtf();
                int sep = raw.indexOf(':');
                SoundId id = sep > 0
                        ? new SoundId(raw.substring(0, sep), raw.substring(sep + 1))
                        : new SoundId("audiolayer", raw);
                return new AudiolayerPlayPacket(id, buf.readInt(), buf.readFloat(), buf.readFloat());
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
