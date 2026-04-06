package com.audiolayer;

import com.audiolayer.api.AudiolayerProvider;
import com.audiolayer.api.ClientAudiolayerApi;
import com.audiolayer.api.SoundId;
import com.audiolayer.commands.AudiolayerCommandSupport;
import com.audiolayer.registry.AudiolayerManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Optional;

public final class AudiolayerClientSetup {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Tracks the Etched SoundInstance that triggered the current MP3 playback.
    // When Etched stops this sound (e.g. disc removed), we stop the MP3 too.
    private SoundInstance pendingEtchedSound = null;

    public AudiolayerClientSetup(IEventBus modEventBus, AudiolayerManager manager) {
        ClientAudiolayerApi api = new ClientAudiolayerApi(manager);
        AudiolayerProvider.register(api);

        NeoForge.EVENT_BUS.addListener((PlaySoundEvent event) -> {
            String rawId = resolveAudiolayerId(event.getSound());
            if (rawId == null) return;

            SoundId id = AudiolayerCommandSupport.parseSoundId(rawId);
            if (!api.isLoaded(id)) {
                LOGGER.debug("Audiolayer: sound event fired for {} but sound is not loaded", rawId);
                return;
            }

            // Do NOT cancel the Etched sound — let it play as a silent EmptyAudioStream.
            // This keeps it registered in SoundEngine.instanceToChannel so Etched's own
            // stop logic fires normally (e.g. when the disc is removed from the jukebox).
            LOGGER.debug("Audiolayer intercepted sound event: {}", id);
            api.play(id);
            pendingEtchedSound = event.getSound();
        });

        // Poll each tick: if the Etched sound is no longer active, stop our MP3.
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            if (pendingEtchedSound == null) return;
            var soundManager = Minecraft.getInstance().getSoundManager();
            if (!soundManager.isActive(pendingEtchedSound)) {
                LOGGER.debug("Audiolayer: Etched sound stopped, stopping MP3 playback");
                api.stop();
                pendingEtchedSound = null;
            }
        });
    }

    /**
     * Returns the audiolayer: sound ID string if this sound instance represents
     * an audiolayer sound, or null otherwise.
     *
     * Handles two cases:
     *  1. Direct – the SoundInstance's own location has namespace "audiolayer".
     *  2. Etched streaming – Etched plays via AbstractOnlineSoundInstance (url field)
     *     wrapped inside StopListeningSound (source field). We unwrap one level and
     *     read the private "url" field via reflection — no hard compile dep on Etched.
     */
    private static String resolveAudiolayerId(SoundInstance sound) {
        if (sound == null) return null;

        // Case 1: direct audiolayer: sound event
        if (sound.getLocation().getNamespace().equals(AudiolayerMod.MODID)) {
            return sound.getLocation().toString();
        }

        // Case 2: Etched StopListeningSound wraps AbstractOnlineSoundInstance.
        // Unwrap "source" field first, then read "url" from the inner instance.
        SoundInstance inner = getField(sound, "source", SoundInstance.class).orElse(sound);
        return getField(inner, "url", String.class)
                .filter(url -> url.startsWith(AudiolayerMod.MODID + ":"))
                .orElse(null);
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
