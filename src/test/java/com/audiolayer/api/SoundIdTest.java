package com.audiolayer.api;

import com.audiolayer.testsupport.TestAssertions;

public final class SoundIdTest {
    public static void run() {
        SoundId id = new SoundId("audiolayer", "music.theme");
        TestAssertions.assertEquals("audiolayer", id.namespace());
        TestAssertions.assertEquals("music.theme", id.path());
        TestAssertions.assertEquals("audiolayer:music.theme", id.toString());

        TestAssertions.assertThrows(NullPointerException.class, () -> new SoundId(null, "x"));
        TestAssertions.assertThrows(NullPointerException.class, () -> new SoundId("x", null));
        TestAssertions.assertThrows(IllegalArgumentException.class, () -> new SoundId("", "x"));
        TestAssertions.assertThrows(IllegalArgumentException.class, () -> new SoundId("x", ""));
    }
}
