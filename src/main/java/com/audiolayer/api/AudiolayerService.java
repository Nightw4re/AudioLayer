package com.audiolayer.api;

import com.audiolayer.audio.LoadedAudioAsset;

import java.util.Optional;
import java.util.Set;

public interface AudiolayerService {
    boolean isLoaded(SoundId id);

    Set<SoundId> listSounds();

    Optional<LoadedAudioAsset> get(SoundId id);

    ReloadSummary reload();
}
