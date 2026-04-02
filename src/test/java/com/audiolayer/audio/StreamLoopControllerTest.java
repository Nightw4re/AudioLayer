package com.audiolayer.audio;

import com.audiolayer.testsupport.TestAssertions;

public final class StreamLoopControllerTest {
    public static void run() {
        // play once, no duration — EOF stops playback
        {
            StreamLoopController c = new StreamLoopController(1, 0f, 0f);
            TestAssertions.assertEquals(StreamLoopController.Action.STOP, c.onEndOfFile());
            TestAssertions.assertTrue(c.isFinished());
        }

        // play 3x, no explicit duration — EOF restarts twice then stops
        {
            StreamLoopController c = new StreamLoopController(3, 0f, 0f);
            TestAssertions.assertEquals(StreamLoopController.Action.RESTART, c.onEndOfFile());
            TestAssertions.assertTrue(!c.isFinished());
            TestAssertions.assertEquals(StreamLoopController.Action.RESTART, c.onEndOfFile());
            TestAssertions.assertTrue(!c.isFinished());
            TestAssertions.assertEquals(StreamLoopController.Action.STOP, c.onEndOfFile());
            TestAssertions.assertTrue(c.isFinished());
        }

        // play 3x with 1s duration starting at 2s — each loop covers [2, 3)
        {
            StreamLoopController c = new StreamLoopController(3, 2f, 1f);
            // advance within segment — no boundary yet
            TestAssertions.assertEquals(StreamLoopController.Action.CONTINUE, c.advance(0.5f));
            TestAssertions.assertTrue(!c.isFinished());
            // cross the 3s boundary — loop 1 ends, restart
            TestAssertions.assertEquals(StreamLoopController.Action.RESTART, c.advance(0.6f));
            TestAssertions.assertTrue(!c.isFinished());
            // second loop — cross boundary again
            TestAssertions.assertEquals(StreamLoopController.Action.CONTINUE, c.advance(0.5f));
            TestAssertions.assertEquals(StreamLoopController.Action.RESTART, c.advance(0.6f));
            TestAssertions.assertTrue(!c.isFinished());
            // third loop — cross boundary → stop
            TestAssertions.assertEquals(StreamLoopController.Action.CONTINUE, c.advance(0.5f));
            TestAssertions.assertEquals(StreamLoopController.Action.STOP, c.advance(0.6f));
            TestAssertions.assertTrue(c.isFinished());
        }

        // infinite loop (count=0) — never stops via EOF
        {
            StreamLoopController c = new StreamLoopController(0, 0f, 0f);
            for (int i = 0; i < 100; i++) {
                TestAssertions.assertEquals(StreamLoopController.Action.RESTART, c.onEndOfFile());
                TestAssertions.assertTrue(!c.isFinished());
            }
        }
    }
}
