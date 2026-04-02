package com.audiolayer.resource;

import com.audiolayer.api.SoundId;
import com.audiolayer.audio.LoadedAudioAsset;
import com.audiolayer.testsupport.TestAssertions;

import java.nio.file.Path;

public final class RuntimeResourcePackWriterTest {
    public static void run() {
        SoundId id = new SoundId("audiolayer", "music.track");
        LoadedAudioAsset asset = new LoadedAudioAsset(
                id,
                Path.of("input/music/track.mp3"),
                "abc123",
                120f
        );

        TestAssertions.assertEquals(id, asset.soundId());
        TestAssertions.assertEquals("abc123", asset.sourceHash());
        TestAssertions.assertEquals(120f, asset.durationSeconds());
        TestAssertions.assertTrue(asset.sourceFile().toString().endsWith("track.mp3"));
    }
}
