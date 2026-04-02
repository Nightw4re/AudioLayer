package com.audiolayer.config;

import com.audiolayer.api.SoundId;
import com.audiolayer.testsupport.TestAssertions;

import java.nio.file.Path;

public final class SoundIdMapperTest {
    public static void run() {
        SoundIdMapper mapper = new SoundIdMapper(new FilenameSanitizer());
        SoundId id1 = mapper.map(Path.of("music/atlantis_intro.mp3"));
        TestAssertions.assertEquals("audiolayer:music.atlantis_intro", id1.toString());

        SoundId id2 = mapper.map(Path.of("Boss Themes/Awakening Phase 1.mp3"));
        TestAssertions.assertEquals("audiolayer:boss_themes.awakening_phase_1", id2.toString());
    }
}
