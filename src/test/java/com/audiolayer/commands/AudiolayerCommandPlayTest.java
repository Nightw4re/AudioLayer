package com.audiolayer.commands;

import com.audiolayer.api.SoundId;
import com.audiolayer.testsupport.TestAssertions;

public final class AudiolayerCommandPlayTest {
    public static void run() {
        SoundId id = new SoundId("audiolayer", "music.track");

        // --- playMessage ---

        // count=1, start=0, duration=0 → no extras
        TestAssertions.assertEquals(
                "Playing: audiolayer:music.track",
                AudiolayerCommandSupport.playMessage(id, 1, 0f, 0f)
        );

        // count=0 → infinite loop label, start=0 shows nothing, duration=0 shows nothing
        TestAssertions.assertEquals(
                "Playing: audiolayer:music.track (looping)",
                AudiolayerCommandSupport.playMessage(id, 0, 0f, 0f)
        );

        // count>1 → repeat label
        TestAssertions.assertEquals(
                "Playing: audiolayer:music.track (x3)",
                AudiolayerCommandSupport.playMessage(id, 3, 0f, 0f)
        );

        // start>0 → shown; start=0 → not shown (covered above)
        TestAssertions.assertEquals(
                "Playing: audiolayer:music.track from 20.0s",
                AudiolayerCommandSupport.playMessage(id, 1, 20f, 0f)
        );

        // duration>0 → shown; duration=0 → not shown (covered above)
        TestAssertions.assertEquals(
                "Playing: audiolayer:music.track for 5.0s",
                AudiolayerCommandSupport.playMessage(id, 1, 0f, 5f)
        );

        // start + duration together
        TestAssertions.assertEquals(
                "Playing: audiolayer:music.track from 20.0s for 5.0s",
                AudiolayerCommandSupport.playMessage(id, 1, 20f, 5f)
        );

        // loop + start + duration together
        TestAssertions.assertEquals(
                "Playing: audiolayer:music.track (x2) from 20.0s for 5.0s",
                AudiolayerCommandSupport.playMessage(id, 2, 20f, 5f)
        );

        // --- shouldLoop ---

        TestAssertions.assertTrue(!AudiolayerCommandSupport.shouldLoop(1));   // play once → no loop
        TestAssertions.assertTrue(AudiolayerCommandSupport.shouldLoop(0));    // count=0 → infinite
        TestAssertions.assertTrue(AudiolayerCommandSupport.shouldLoop(2));    // count>1 → loop
        TestAssertions.assertTrue(AudiolayerCommandSupport.shouldLoop(99));

        // --- durationTicks ---

        // count=0 → always -1, even when duration or asset duration specified
        TestAssertions.assertEquals(-1, AudiolayerCommandSupport.durationTicks(0, 0f, 0f));
        TestAssertions.assertEquals(-1, AudiolayerCommandSupport.durationTicks(0, 0f, 120f));
        TestAssertions.assertEquals(-1, AudiolayerCommandSupport.durationTicks(0, 5f, 120f));

        // count=1, duration=0 → -1 (play to end of file)
        TestAssertions.assertEquals(-1, AudiolayerCommandSupport.durationTicks(1, 0f, 0f));
        TestAssertions.assertEquals(-1, AudiolayerCommandSupport.durationTicks(1, 0f, 120f));

        // explicit duration: total = duration * count
        TestAssertions.assertEquals(100, AudiolayerCommandSupport.durationTicks(1, 5f, 120f));  // 1 * 5s = 5s
        TestAssertions.assertEquals(200, AudiolayerCommandSupport.durationTicks(2, 5f, 120f));  // 2 * 5s = 10s
        TestAssertions.assertEquals(240, AudiolayerCommandSupport.durationTicks(4, 3f, 120f));  // 4 * 3s = 12s

        // count>1, duration=0, known asset → asset*count
        TestAssertions.assertEquals(4800, AudiolayerCommandSupport.durationTicks(2, 0f, 120f));

        // count>1, duration=0, unknown asset → -1 (can't compute, play indefinitely)
        TestAssertions.assertEquals(-1, AudiolayerCommandSupport.durationTicks(3, 0f, 0f));

        // --- loopDurationTicks ---

        // count=1 → no looping → -1
        TestAssertions.assertEquals(-1, AudiolayerCommandSupport.loopDurationTicks(1, 3f, 120f));
        TestAssertions.assertEquals(-1, AudiolayerCommandSupport.loopDurationTicks(1, 0f, 120f));

        // count=0 (infinite) → -1 (no re-seek needed, no end boundary)
        TestAssertions.assertEquals(-1, AudiolayerCommandSupport.loopDurationTicks(0, 3f, 120f));

        // count>1, explicit duration → per-loop = durationSeconds
        TestAssertions.assertEquals(60,  AudiolayerCommandSupport.loopDurationTicks(4, 3f, 120f));  // 3s = 60 ticks
        TestAssertions.assertEquals(100, AudiolayerCommandSupport.loopDurationTicks(2, 5f, 120f));  // 5s = 100 ticks

        // count>1, no duration → per-loop = assetDuration
        TestAssertions.assertEquals(2400, AudiolayerCommandSupport.loopDurationTicks(2, 0f, 120f)); // 120s = 2400 ticks

        // count>1, no duration, no asset → -1
        TestAssertions.assertEquals(-1, AudiolayerCommandSupport.loopDurationTicks(2, 0f, 0f));
    }
}
