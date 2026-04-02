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
    }
}
