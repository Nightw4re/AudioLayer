package com.audiolayer.api;

import com.audiolayer.audio.Mp3SoundInstance;
import com.audiolayer.registry.AudiolayerManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ClientAudiolayerApi implements AudiolayerApi {
    private final AudiolayerManager manager;
    private final Map<String, Mp3SoundInstance> instancesByChannel = new LinkedHashMap<>();

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
        play(id, count, startSeconds, durationSeconds, 1f, 1f, "master");
    }

    @Override
    public void play(
            SoundId id,
            int count,
            float startSeconds,
            float durationSeconds,
            float volume,
            float pitch,
            String category
    ) {
        manager.get(id).ifPresent(asset -> {
            String channel = normalizeChannel(category);
            stopChannel(channel);
            Mp3SoundInstance instance = new Mp3SoundInstance(asset.sourceFile(), count, startSeconds, durationSeconds, volume, pitch);
            instancesByChannel.put(channel, instance);
            instance.play();
        });
    }

    @Override
    public void stop() {
        instancesByChannel.keySet().stream().toList().forEach(this::stopChannel);
    }

    @Override
    public void stop(String category) {
        stopChannel(normalizeChannel(category));
    }

    private void stopChannel(String channel) {
        Mp3SoundInstance instance = instancesByChannel.remove(channel);
        if (instance != null && !instance.isStopped()) {
            instance.stop();
        }
    }

    private String normalizeChannel(String category) {
        if (category == null || category.isBlank()) return "master";
        return category.trim().toLowerCase();
    }
}
