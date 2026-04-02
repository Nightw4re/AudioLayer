package com.audiolayer.commands;

import com.audiolayer.api.SoundId;

import java.util.Collection;
import java.util.List;

public final class AudiolayerCommandSupport {
    private AudiolayerCommandSupport() {
    }

    public static SoundId parseSoundId(String raw) {
        int separator = raw.indexOf(':');
        if (separator <= 0 || separator == raw.length() - 1) {
            return new SoundId("audiolayer", raw);
        }
        return new SoundId(raw.substring(0, separator), raw.substring(separator + 1));
    }

    public static List<String> suggestSoundIds(Collection<SoundId> soundIds) {
        return soundIds.stream().map(SoundId::toString).sorted().toList();
    }

    public static String reloadMessage(int loadedAssets, int reusedAssets, int failedFiles) {
        return "Reloaded " + loadedAssets + " sound(s), reused " + reusedAssets + ", failed " + failedFiles;
    }

    public static String debugMessage(boolean sourceExists, boolean cacheExists, boolean packDirExists) {
        return "sourceExists=" + sourceExists + ", cacheExists=" + cacheExists + ", packDirExists=" + packDirExists;
    }

    public static String playMessage(SoundId id, int count, float startSeconds, float durationSeconds) {
        StringBuilder sb = new StringBuilder("Playing: ").append(id);
        if (count == 0) sb.append(" (looping)");
        else if (count > 1) sb.append(" (x").append(count).append(")");
        if (startSeconds > 0) sb.append(" from ").append(startSeconds).append("s");
        if (durationSeconds > 0) sb.append(" for ").append(durationSeconds).append("s");
        return sb.toString();
    }

    public static boolean shouldLoop(int count) {
        return count == 0 || count > 1;
    }

    public static int durationTicks(int count, float durationSeconds, float assetDurationSeconds) {
        if (count == 0) return -1; // infinite loop — never stop
        if (durationSeconds > 0) return Math.round(durationSeconds * count * 20); // total = per-loop * count
        if (count > 1 && assetDurationSeconds > 0) return Math.round(assetDurationSeconds * count * 20);
        return -1;
    }

    /** Ticks per single loop iteration — used to re-seek to startSeconds after each loop. -1 = not applicable. */
    public static int loopDurationTicks(int count, float durationSeconds, float assetDurationSeconds) {
        if (count <= 1) return -1;
        if (durationSeconds > 0) return Math.round(durationSeconds * 20);
        if (assetDurationSeconds > 0) return Math.round(assetDurationSeconds * 20);
        return -1;
    }
}
