package com.audiolayer.audio;

/**
 * Tracks loop state for streamed audio playback.
 *
 * <p>A "loop" here means one play-through of the segment
 * [startSeconds, startSeconds + durationSeconds). When durationSeconds == 0
 * the segment ends at the natural end of the file.
 *
 * <p>count == 0 means infinite looping.
 */
public final class StreamLoopController {
    private final float startSeconds;
    private final float endSeconds;   // Float.MAX_VALUE when no duration limit
    private int loopsRemaining;       // Integer.MAX_VALUE when infinite

    private float secondsStreamed;
    private boolean finished = false;

    public StreamLoopController(int count, float startSeconds, float durationSeconds) {
        this.startSeconds = startSeconds;
        this.endSeconds = durationSeconds > 0 ? startSeconds + durationSeconds : Float.MAX_VALUE;
        this.loopsRemaining = count == 0 ? Integer.MAX_VALUE : count;
        this.secondsStreamed = startSeconds;
    }

    /**
     * Called after each decoded chunk is consumed.
     *
     * @param chunkSeconds duration of the chunk just appended to the stream
     * @return action to take
     */
    public Action advance(float chunkSeconds) {
        secondsStreamed += chunkSeconds;
        if (secondsStreamed >= endSeconds) {
            return onBoundaryReached();
        }
        return Action.CONTINUE;
    }

    /**
     * Called when the decoder hits end-of-file before the duration limit.
     */
    public Action onEndOfFile() {
        return onBoundaryReached();
    }

    private Action onBoundaryReached() {
        if (loopsRemaining == Integer.MAX_VALUE) {
            secondsStreamed = startSeconds;
            return Action.RESTART;
        }
        loopsRemaining--;
        if (loopsRemaining <= 0) {
            finished = true;
            return Action.STOP;
        }
        secondsStreamed = startSeconds;
        return Action.RESTART;
    }

    public boolean isFinished() { return finished; }
    public float startSeconds()  { return startSeconds; }

    public enum Action { CONTINUE, RESTART, STOP }
}
