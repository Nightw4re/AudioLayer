package com.audiolayer.api;

import com.audiolayer.audio.AudiolayerSoundInstance;
import com.audiolayer.commands.AudiolayerCommandSupport;
import com.audiolayer.registry.AudiolayerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class ClientAudiolayerApi implements AudiolayerApi {
    private final AudiolayerManager manager;
    private AudiolayerSoundInstance currentInstance;

    public ClientAudiolayerApi(AudiolayerManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean isLoaded(SoundId id) {
        return manager.isLoaded(id);
    }

    @Override
    public Set<SoundId> listSounds() {
        return manager.listSounds();
    }

    @Override
    public java.util.Optional<com.audiolayer.audio.LoadedAudioAsset> get(SoundId id) {
        return manager.get(id);
    }

    @Override
    public void reload() {
        manager.reload();
    }

    @Override
    public void play(SoundId id) {
        play(id, 1, 0f, 0f);
    }

    @Override
    public void play(SoundId id, int count, float startSeconds, float durationSeconds) {
        manager.get(id).ifPresent(asset -> {
            stopCurrent();
            boolean loop = AudiolayerCommandSupport.shouldLoop(count);
            int durationTicks = AudiolayerCommandSupport.durationTicks(count, durationSeconds, asset.durationSeconds());
            int loopDurationTicks = AudiolayerCommandSupport.loopDurationTicks(count, durationSeconds, asset.durationSeconds());
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(id.namespace(), id.path());
            currentInstance = new AudiolayerSoundInstance(location, loop, startSeconds, durationTicks, loopDurationTicks);
            Minecraft.getInstance().getSoundManager().play(currentInstance);
        });
    }

    @Override
    public void stop() {
        stopCurrent();
    }

    private void stopCurrent() {
        if (currentInstance != null) {
            currentInstance.forceStop();
            Minecraft.getInstance().getSoundManager().stop(currentInstance);
            currentInstance = null;
        }
    }
}
