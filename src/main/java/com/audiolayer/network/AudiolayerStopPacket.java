package com.audiolayer.network;

import com.audiolayer.AudiolayerMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AudiolayerStopPacket() implements CustomPacketPayload {

    public static final Type<AudiolayerStopPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AudiolayerMod.MODID, "stop")
    );

    public static final StreamCodec<FriendlyByteBuf, AudiolayerStopPacket> CODEC =
            StreamCodec.of((buf, pkt) -> {}, buf -> new AudiolayerStopPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
