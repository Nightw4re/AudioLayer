package com.audiolayer.commands;

import com.audiolayer.testsupport.TestAssertions;

public final class AudiolayerCommandDebugTest {
    public static void run() {
        TestAssertions.assertEquals(
                "sourceExists=true, cacheExists=false, packDirExists=true",
                AudiolayerCommandSupport.debugMessage(true, false, true)
        );
    }
}
