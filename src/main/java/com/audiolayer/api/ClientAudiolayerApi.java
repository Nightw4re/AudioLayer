package com.audiolayer.api;

import com.audiolayer.audio.Mp3SoundInstance;
import com.audiolayer.commands.AudiolayerCommandSupport;
import com.audiolayer.registry.AudiolayerManager;

import java.util.Optional;
import java.util.Set;

public final class ClientAudiolayerApi implements AudiolayerApi {
    private final AudiolayerManager manager;
    private Mp3SoundInstance currentInstance;

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
    public Optional<com.audiolayer.audio.LoadedAudioAsset> get(SoundId id) {
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
            currentInstance = new Mp3SoundInstance(asset.sourceFile(), count, startSeconds, durationSeconds);
            currentInstance.play();
        });
    }

    @Override
    public void stop() {
        stopCurrent();
    }

    private void stopCurrent() {
        if (currentInstance != null && !currentInstance.isStopped()) {
            currentInstance.stop();
        }
        currentInstance = null;
    }
}
