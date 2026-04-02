package com.audiolayer.resource;

import com.audiolayer.api.SoundId;
import com.audiolayer.audio.LoadedAudioAsset;
import com.audiolayer.testsupport.TestAssertions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class RuntimeResourcePackWriterTest {
    public static void run() throws Exception {
        Path root = Files.createTempDirectory("audiolayer-pack");
        Path packRoot = root.resolve("pack");
        RuntimeResourcePackWriter writer = new RuntimeResourcePackWriter(packRoot);

        // create a fake OGG cache file
        Path fakeOgg = root.resolve("abc123.ogg");
        Files.writeString(fakeOgg, "fake");

        LoadedAudioAsset asset = new LoadedAudioAsset(
                new SoundId("audiolayer", "music.track"),
                Path.of("input/music/track.mp3"),
                fakeOgg,
                "abc123",
                120f
        );
        writer.write(List.of(asset));

        // sounds.json exists
        Path soundsJson = packRoot.resolve("assets/audiolayer/sounds.json");
        TestAssertions.assertTrue(Files.exists(soundsJson));

        String json = Files.readString(soundsJson);

        // entry key matches sound path
        TestAssertions.assertTrue(json.contains("\"music.track\""));

        // name uses underscore variant (dot replaced)
        TestAssertions.assertTrue(json.contains("audiolayer:music_track"));

        // preload is set
        TestAssertions.assertTrue(json.contains("\"preload\":true"));

        // stream is NOT set
        TestAssertions.assertTrue(!json.contains("\"stream\""));

        // pack.mcmeta exists
        TestAssertions.assertTrue(Files.exists(packRoot.resolve("pack.mcmeta")));

        // OGG copied to pack
        TestAssertions.assertTrue(Files.exists(packRoot.resolve("assets/audiolayer/sounds/music_track.ogg")));
    }
}
