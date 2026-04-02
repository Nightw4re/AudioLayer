package com.audiolayer.commands;

import com.audiolayer.api.SoundId;
import com.audiolayer.testsupport.TestAssertions;

import java.util.List;

public final class AudiolayerCommandSupportTest {
    public static void run() {
        TestAssertions.assertEquals("audiolayer:starfall_armada", AudiolayerCommandSupport.parseSoundId("audiolayer:starfall_armada").toString());
        TestAssertions.assertEquals("audiolayer:starfall_armada", AudiolayerCommandSupport.parseSoundId("starfall_armada").toString());
        List<String> suggestions = AudiolayerCommandSupport.suggestSoundIds(List.of(new SoundId("audiolayer", "b"), new SoundId("audiolayer", "a")));
        TestAssertions.assertEquals(List.of("audiolayer:a", "audiolayer:b"), suggestions);
        TestAssertions.assertEquals("Reloaded 2 sound(s), reused 1, failed 0", AudiolayerCommandSupport.reloadMessage(2, 1, 0));
        TestAssertions.assertEquals("sourceExists=true, cacheExists=false, packDirExists=true", AudiolayerCommandSupport.debugMessage(true, false, true));
        TestAssertions.assertEquals("Playing: audiolayer:music.theme", AudiolayerCommandSupport.playMessage(new SoundId("audiolayer", "music.theme"), 1, 0f, 0f));
        TestAssertions.assertEquals("Playing: audiolayer:music.theme (looping) from 5.0s for 30.0s", AudiolayerCommandSupport.playMessage(new SoundId("audiolayer", "music.theme"), 0, 5f, 30f));
        TestAssertions.assertTrue(AudiolayerCommandSupport.shouldLoop(0));
        TestAssertions.assertTrue(AudiolayerCommandSupport.shouldLoop(2));
        TestAssertions.assertEquals(-1, AudiolayerCommandSupport.durationTicks(0, 0f, 12f));
        TestAssertions.assertEquals(240, AudiolayerCommandSupport.durationTicks(2, 6f, 12f));
        TestAssertions.assertEquals(240, AudiolayerCommandSupport.loopDurationTicks(2, 0f, 12f));
    }
}
