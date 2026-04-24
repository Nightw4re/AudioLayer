package com.audiolayer.network;

import com.audiolayer.AudiolayerMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AudiolayerStopPacket(String category) implements CustomPacketPayload {

    public static final Type<AudiolayerStopPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AudiolayerMod.MODID, "stop")
    );

    public AudiolayerStopPacket() {
        this("");
    }

    public static final StreamCodec<FriendlyByteBuf, AudiolayerStopPacket> CODEC =
            StreamCodec.of((buf, pkt) -> buf.writeUtf(pkt.category()), buf -> new AudiolayerStopPacket(buf.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
