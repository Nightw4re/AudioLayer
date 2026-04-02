package com.audiolayer.commands;

import com.audiolayer.testsupport.TestAssertions;

public final class AudiolayerCommandReloadTest {
    public static void run() {
        TestAssertions.assertEquals("Reloaded 3 sound(s), reused 2, failed 1", AudiolayerCommandSupport.reloadMessage(3, 2, 1));
    }
}
