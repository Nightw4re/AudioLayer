package com.audiolayer.audio;

public final class MusicRoutingRules {
    private MusicRoutingRules() {}

    public static boolean shouldRouteMenuMusic(Object level) {
        return level == null;
    }
}
