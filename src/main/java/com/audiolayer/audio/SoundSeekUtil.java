package com.audiolayer.audio;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Map;

public final class SoundSeekUtil {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Field soundEngineField;
    private static Field instanceToChannelField;
    private static Field channelHandleChannelField;
    private static Field channelSourceField;

    public static boolean isChannelReady(SoundInstance instance) {
        try {
            Map<?, ?> map = getInstanceToChannel();
            return map != null && map.containsKey(instance);
        } catch (Exception e) {
            return false;
        }
    }

    public static void seekTo(SoundInstance instance, float seconds) {
        withSource(instance, sourceId -> AL10.alSourcef(sourceId, AL11.AL_SEC_OFFSET, seconds),
                "seek to " + seconds + "s");
    }

    public static void setLooping(SoundInstance instance, boolean loop) {
        withSource(instance, sourceId -> AL10.alSourcei(sourceId, AL10.AL_LOOPING, loop ? AL10.AL_TRUE : AL10.AL_FALSE),
                "setLooping=" + loop);
    }

    private static Map<?, ?> getInstanceToChannel() throws Exception {
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        if (soundEngineField == null) {
            soundEngineField = SoundManager.class.getDeclaredField("soundEngine");
            soundEngineField.setAccessible(true);
        }
        Object soundEngine = soundEngineField.get(sm);
        if (instanceToChannelField == null) {
            instanceToChannelField = soundEngine.getClass().getDeclaredField("instanceToChannel");
            instanceToChannelField.setAccessible(true);
        }
        return (Map<?, ?>) instanceToChannelField.get(soundEngine);
    }

    private static void withSource(SoundInstance instance, java.util.function.IntConsumer action, String opName) {
        try {
            Map<?, ?> map = getInstanceToChannel();
            Object handle = map.get(instance);
            if (handle == null) {
                LOGGER.warn("Audiolayer: no channel for {}", opName);
                return;
            }
            if (channelHandleChannelField == null) {
                channelHandleChannelField = handle.getClass().getDeclaredField("channel");
                channelHandleChannelField.setAccessible(true);
            }
            Object channel = channelHandleChannelField.get(handle);
            if (channelSourceField == null) {
                channelSourceField = channel.getClass().getDeclaredField("source");
                channelSourceField.setAccessible(true);
            }
            int sourceId = (int) channelSourceField.get(channel);
            action.accept(sourceId);
        } catch (Exception e) {
            LOGGER.warn("Audiolayer: {} failed: {}", opName, e.getMessage());
        }
    }
}
