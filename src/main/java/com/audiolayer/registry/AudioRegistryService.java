package com.audiolayer.registry;

import com.audiolayer.api.SoundId;
import com.audiolayer.audio.AudioSourceDescriptor;
import com.audiolayer.audio.LoadedAudioAsset;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AudioRegistryService {
    private final Map<SoundId, LoadedAudioAsset> assets = new LinkedHashMap<>();

    public void rebuild(Collection<AudioSourceDescriptor> descriptors) {
        assets.clear();
        for (AudioSourceDescriptor descriptor : descriptors) {
            assets.put(descriptor.soundId(), new LoadedAudioAsset(
                    descriptor.soundId(),
                    descriptor.absolutePath(),
                    descriptor.contentHash(),
                    0f
            ));
        }
    }

    public boolean isLoaded(SoundId id) {
        return assets.containsKey(id);
    }

    public Set<SoundId> listSounds() {
        return Set.copyOf(assets.keySet());
    }

    public Optional<LoadedAudioAsset> get(SoundId id) {
        return Optional.ofNullable(assets.get(id));
    }
}
