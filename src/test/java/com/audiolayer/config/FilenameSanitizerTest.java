package com.audiolayer.config;

import com.audiolayer.testsupport.TestAssertions;

public final class FilenameSanitizerTest {
    public static void run() {
        FilenameSanitizer sanitizer = new FilenameSanitizer();
        TestAssertions.assertEquals("boss_themes", sanitizer.sanitizeSegment("Boss Themes"));
        TestAssertions.assertEquals("awakening_phase_1", sanitizer.sanitizeSegment("Awakening Phase 1"));
        TestAssertions.assertThrows(IllegalArgumentException.class, () -> sanitizer.sanitizeSegment("!!!"));
    }
}
