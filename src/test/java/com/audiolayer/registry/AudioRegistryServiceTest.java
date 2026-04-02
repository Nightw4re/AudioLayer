package com.audiolayer.registry;

import com.audiolayer.api.SoundId;
import com.audiolayer.audio.AudioSourceDescriptor;
import com.audiolayer.testsupport.TestAssertions;

import java.nio.file.Path;
import java.util.List;

public final class AudioRegistryServiceTest {
    public static void run() {
        AudioRegistryService registry = new AudioRegistryService();
        registry.rebuild(List.of(new AudioSourceDescriptor(
                Path.of("input/music/atlantis_intro.mp3"),
                "music/atlantis_intro.mp3",
                "mp3",
                12L,
                "hash",
                "music",
                "atlantis_intro",
                new SoundId("audiolayer", "music.atlantis_intro")
        )));

        TestAssertions.assertTrue(registry.isLoaded(new SoundId("audiolayer", "music.atlantis_intro")));
        TestAssertions.assertEquals(1, registry.listSounds().size());
    }
}
