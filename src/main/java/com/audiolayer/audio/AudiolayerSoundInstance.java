package com.audiolayer.audio;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class AudiolayerSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {
    private final float startSeconds;
    private final int durationTicks;     // -1 = unlimited
    private final int loopDurationTicks; // -1 = no re-seek needed
    private int ticksPlayed = 0;
    private boolean stopped = false;
    private boolean seekApplied = false;

    public AudiolayerSoundInstance(ResourceLocation location, boolean loop, float startSeconds, int durationTicks, int loopDurationTicks) {
        super(SoundEvent.createVariableRangeEvent(location), SoundSource.RECORDS, RandomSource.create());
        this.looping = loop;
        this.relative = true;
        this.volume = 1.0f;
        this.pitch = 1.0f;
        this.startSeconds = startSeconds;
        this.durationTicks = durationTicks;
        this.loopDurationTicks = loopDurationTicks;
    }

    @Override
    public void tick() {
        if (!seekApplied) {
            boolean channelReady = SoundSeekUtil.isChannelReady(this);
            if (channelReady) {
                if (looping) {
                    SoundSeekUtil.setLooping(this, true);
                }
                if (startSeconds > 0) {
                    SoundSeekUtil.seekTo(this, startSeconds);
                }
                seekApplied = true;
            }
        }
        if (durationTicks > 0 || loopDurationTicks > 0) {
            ticksPlayed++;
            if (loopDurationTicks > 0 && startSeconds > 0 && ticksPlayed % loopDurationTicks == 0) {
                SoundSeekUtil.seekTo(this, startSeconds);
            }
            if (durationTicks > 0 && ticksPlayed >= durationTicks) {
                stopped = true;
            }
        }
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    public void forceStop() {
        stopped = true;
    }

    @Override
    public SoundInstance.Attenuation getAttenuation() {
        return SoundInstance.Attenuation.NONE;
    }
}
