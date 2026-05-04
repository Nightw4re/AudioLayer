package com.audiolayer.audio;

import com.audiolayer.testsupport.TestAssertions;

public final class MusicRoutingRulesTest {
    public static void run() {
        TestAssertions.assertTrue(MusicRoutingRules.shouldRouteMenuMusic(null));
        TestAssertions.assertTrue(!MusicRoutingRules.shouldRouteMenuMusic(new Object()));
    }
}
