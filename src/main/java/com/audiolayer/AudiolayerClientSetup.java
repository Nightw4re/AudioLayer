package com.audiolayer;

import com.audiolayer.api.AudiolayerProvider;
import com.audiolayer.api.ClientAudiolayerApi;
import com.audiolayer.audio.AudiolayerSoundEventHandler;
import com.audiolayer.audio.HashService;
import com.audiolayer.audio.MusicRoutingRules;
import com.audiolayer.audio.ResourcePackAudioScanner;
import com.audiolayer.registry.AudiolayerManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.nio.file.Path;

public final class AudiolayerClientSetup {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MUSIC_CATEGORY = "music";
    private static final String ETCHED_CATEGORY = "records";
    private final AudiolayerSoundEventHandler soundHandler;
    private boolean wasInMenuContext = false;

    public AudiolayerClientSetup(IEventBus modEventBus, AudiolayerManager manager) {
        ClientAudiolayerApi api = new ClientAudiolayerApi(manager);
        this.soundHandler = new AudiolayerSoundEventHandler(api);
        AudiolayerProvider.register(api);
        manager.setAdditionalSourceScanner(this::scanResourcePackSounds);
        manager.reload();

        registerSoundReplacementHook(soundHandler);
        registerEtchedPlaybackHook();
        registerMenuMusicVolumeHook(api);
    }

    private java.util.List<com.audiolayer.audio.AudioSourceDescriptor> scanResourcePackSounds() {
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            Path extractionDir = Path.of("config", "audiolayer", "cache", "resource-pack");
            return new ResourcePackAudioScanner(
                    new com.audiolayer.config.SoundIdMapper(new com.audiolayer.config.FilenameSanitizer()),
                    new HashService(),
                    extractionDir
            ).scan(resourceManager);
        } catch (Exception e) {
            LOGGER.warn("Audiolayer: failed to scan resource packs: {}", e.getMessage());
            return java.util.List.of();
        }
    }

    private void registerSoundReplacementHook(AudiolayerSoundEventHandler soundHandler) {
        NeoForge.EVENT_BUS.addListener((PlaySoundEvent event) -> {
            soundHandler.handle(event);
        });
    }

    private void registerEtchedPlaybackHook() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            soundHandler.tickEtchedPlayback();
        });
    }

    private void registerMenuMusicVolumeHook(ClientAudiolayerApi api) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            boolean inMenuContext = MusicRoutingRules.shouldRouteMenuMusic(Minecraft.getInstance().level);
            if (inMenuContext != wasInMenuContext) {
                api.stop(MUSIC_CATEGORY);
            }
            wasInMenuContext = inMenuContext;
            soundHandler.syncMenuMusicVolume(wasInMenuContext);
        });
    }
}
