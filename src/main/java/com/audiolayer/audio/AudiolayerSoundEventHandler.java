package com.audiolayer.audio;

import com.audiolayer.AudiolayerMod;
import com.audiolayer.api.ClientAudiolayerApi;
import com.audiolayer.api.SoundId;
import com.audiolayer.commands.AudiolayerCommandSupport;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Optional;

public final class AudiolayerSoundEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ETCHED_CATEGORY = "records";
    private static final String MUSIC_CATEGORY = "music";

    private final ClientAudiolayerApi api;
    private SoundInstance pendingEtchedSound;

    public AudiolayerSoundEventHandler(ClientAudiolayerApi api) {
        this.api = api;
    }

    public void handle(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;

        String rawId = resolveAudiolayerId(sound);
        if (rawId != null) {
            SoundId id = AudiolayerCommandSupport.parseSoundId(rawId);
            if (!api.isLoaded(id)) {
                LOGGER.debug("Audiolayer: sound event fired for {} but sound is not loaded", rawId);
                return;
            }

            LOGGER.debug("Audiolayer intercepted sound event: {}", id);
            api.play(id, 1, 0f, 0f, 1f, 1f, ETCHED_CATEGORY);
            pendingEtchedSound = sound;
            event.setSound(null);
            return;
        }

        if (sound.getSource() == SoundSource.MUSIC) {
            if (!MusicRoutingRules.shouldRouteMenuMusic(Minecraft.getInstance().level)) {
                return;
            }
            SoundId musicId = resolveMusicId(sound.getLocation());
            if (musicId != null && api.isLoaded(musicId)) {
                LOGGER.debug("Audiolayer replacing vanilla music {} with {}", sound.getLocation(), musicId);
                float musicVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC);
                api.play(musicId, 1, 0f, 0f, musicVolume, 1f, MUSIC_CATEGORY);
                event.setSound(null);
            }
        }
    }

    public void tickEtchedPlayback() {
        if (pendingEtchedSound == null) return;
        var soundManager = Minecraft.getInstance().getSoundManager();
        if (!soundManager.isActive(pendingEtchedSound)) {
            LOGGER.debug("Audiolayer: Etched sound stopped, stopping MP3 playback");
            api.stop(ETCHED_CATEGORY);
            pendingEtchedSound = null;
        }
    }

    public void syncMenuMusicVolume(boolean previousMenuContext) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean inMenuContext = MusicRoutingRules.shouldRouteMenuMusic(minecraft.level);
        if (inMenuContext != previousMenuContext) {
            api.stop(MUSIC_CATEGORY);
        }
        api.setVolume(MUSIC_CATEGORY, minecraft.options.getSoundSourceVolume(SoundSource.MUSIC));
        api.setVolume(ETCHED_CATEGORY, minecraft.options.getSoundSourceVolume(SoundSource.RECORDS));
    }

    public boolean isInMenuContext() {
        return MusicRoutingRules.shouldRouteMenuMusic(Minecraft.getInstance().level);
    }

    private static String resolveAudiolayerId(SoundInstance sound) {
        if (sound == null) return null;
        if (sound.getLocation().getNamespace().equals(AudiolayerMod.MODID)) {
            return sound.getLocation().toString();
        }
        SoundInstance inner = getField(sound, "source", SoundInstance.class).orElse(sound);
        return getField(inner, "url", String.class)
                .filter(url -> url.startsWith(AudiolayerMod.MODID + ":"))
                .orElse(null);
    }

    private static SoundId resolveMusicId(ResourceLocation location) {
        if (!"minecraft".equals(location.getNamespace())) return null;
        String path = location.getPath();
        if (!path.startsWith("music.")) return null;
        return new SoundId(AudiolayerMod.MODID, path);
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> getField(Object obj, String name, Class<T> type) {
        Class<?> cls = obj.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                Object val = f.get(obj);
                return type.isInstance(val) ? Optional.of((T) val) : Optional.empty();
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            } catch (IllegalAccessException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
