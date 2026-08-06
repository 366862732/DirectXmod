/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class JukeboxSongPlayer {
    public static final int PLAY_EVENT_INTERVAL_TICKS = 20;
    private long ticksSinceSongStarted;
    private @Nullable Holder<JukeboxSong> song;
    private final BlockPos blockPos;
    private final OnSongChanged onSongChanged;

    public JukeboxSongPlayer(OnSongChanged onSongChanged, BlockPos blockPos) {
        this.onSongChanged = onSongChanged;
        this.blockPos = blockPos;
    }

    public boolean isPlaying() {
        return this.song != null;
    }

    public @Nullable JukeboxSong getSong() {
        if (this.song == null) {
            return null;
        }
        return this.song.value();
    }

    public long getTicksSinceSongStarted() {
        return this.ticksSinceSongStarted;
    }

    public void setSongWithoutPlaying(Holder<JukeboxSong> song, long ticksSinceSongStarted) {
        if (song.value().hasFinished(ticksSinceSongStarted)) {
            return;
        }
        this.song = song;
        this.ticksSinceSongStarted = ticksSinceSongStarted;
    }

    public void play(LevelAccessor level, Holder<JukeboxSong> song) {
        this.song = song;
        this.ticksSinceSongStarted = 0L;
        int songId = level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getId(this.song.value());
        level.levelEvent(null, 1010, this.blockPos, songId);
        this.onSongChanged.notifyChange();
    }

    public void stop(LevelAccessor level, @Nullable BlockState blockState) {
        if (this.song == null) {
            return;
        }
        this.song = null;
        this.ticksSinceSongStarted = 0L;
        level.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, this.blockPos, GameEvent.Context.of(blockState));
        level.levelEvent(1011, this.blockPos, 0);
        this.onSongChanged.notifyChange();
    }

    public void tick(LevelAccessor level, @Nullable BlockState blockState) {
        if (this.song == null) {
            return;
        }
        if (this.song.value().hasFinished(this.ticksSinceSongStarted)) {
            this.stop(level, blockState);
            return;
        }
        if (this.shouldEmitJukeboxPlayingEvent()) {
            level.gameEvent(GameEvent.JUKEBOX_PLAY, this.blockPos, GameEvent.Context.of(blockState));
            JukeboxSongPlayer.spawnMusicParticles(level, this.blockPos);
        }
        ++this.ticksSinceSongStarted;
    }

    private boolean shouldEmitJukeboxPlayingEvent() {
        return this.ticksSinceSongStarted % 20L == 0L;
    }

    private static void spawnMusicParticles(LevelAccessor level, BlockPos blockPos) {
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            Vec3 pos = Vec3.atBottomCenterOf(blockPos).add(0.0, 1.2f, 0.0);
            float randomColor = (float)level.getRandom().nextInt(4) / 24.0f;
            serverLevel.sendParticles(ParticleTypes.NOTE, pos.x(), pos.y(), pos.z(), 0, randomColor, 0.0, 0.0, 1.0);
        }
    }

    @FunctionalInterface
    public static interface OnSongChanged {
        public void notifyChange();
    }
}

