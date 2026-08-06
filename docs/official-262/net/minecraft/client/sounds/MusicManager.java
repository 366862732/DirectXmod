/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.sounds;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class MusicManager {
    private static final int STARTING_DELAY = 100;
    private final RandomSource random = RandomSource.create();
    private final Minecraft minecraft;
    private @Nullable SoundInstance currentMusic;
    private MusicFrequency gameMusicFrequency;
    private float currentGain = 1.0f;
    private int nextSongDelay = 100;
    private boolean toastShown = false;

    public MusicManager(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.gameMusicFrequency = minecraft.options.musicFrequency().get();
    }

    public void tick() {
        boolean stillPlaying;
        float volume = this.minecraft.getMusicVolume();
        if (this.currentMusic != null && this.currentGain != volume && !(stillPlaying = this.fadePlaying(volume))) {
            return;
        }
        Music music = this.minecraft.getSituationalMusic();
        if (music == null) {
            this.nextSongDelay = Math.max(this.nextSongDelay, 100);
            return;
        }
        if (this.currentMusic != null) {
            if (MusicManager.canReplace(music, this.currentMusic)) {
                this.minecraft.getSoundManager().stop(this.currentMusic);
                this.nextSongDelay = Mth.nextInt(this.random, 0, music.minDelay() / 2);
            }
            if (!this.minecraft.getSoundManager().isActive(this.currentMusic)) {
                this.currentMusic = null;
                this.nextSongDelay = Math.min(this.nextSongDelay, this.gameMusicFrequency.getNextSongDelay(music, this.random));
            }
        }
        this.nextSongDelay = Math.min(this.nextSongDelay, music.maxDelay());
        if (this.currentMusic == null && !(this.minecraft.gui.screen() instanceof LevelLoadingScreen) && --this.nextSongDelay <= 0) {
            this.startPlaying(music);
        }
    }

    private static boolean canReplace(Music music, SoundInstance currentMusic) {
        return music.replaceCurrentMusic() && !music.sound().value().location().equals(currentMusic.getIdentifier());
    }

    public void startPlaying(Music music) {
        SoundEvent soundEvent = music.sound().value();
        this.currentMusic = SimpleSoundInstance.forMusic(soundEvent);
        switch (this.minecraft.getSoundManager().play(this.currentMusic)) {
            case STARTED: {
                this.minecraft.gui.toastManager().showNowPlayingToast();
                this.toastShown = true;
                break;
            }
            case STARTED_SILENTLY: {
                this.toastShown = false;
            }
        }
        this.nextSongDelay = Integer.MAX_VALUE;
    }

    public void showNowPlayingToastIfNeeded() {
        if (!this.toastShown) {
            this.minecraft.gui.toastManager().showNowPlayingToast();
            this.toastShown = true;
        }
    }

    public void stopPlaying(Music music) {
        if (this.isPlayingMusic(music)) {
            this.stopPlaying();
        }
    }

    public void stopPlaying() {
        if (this.currentMusic != null) {
            this.minecraft.getSoundManager().stop(this.currentMusic);
            this.currentMusic = null;
            this.minecraft.gui.toastManager().hideNowPlayingToast();
        }
        this.nextSongDelay = this.gameMusicFrequency.getNextSongDelay(this.minecraft.getSituationalMusic(), this.random) + 100;
    }

    private boolean fadePlaying(float volume) {
        if (this.currentMusic == null) {
            return false;
        }
        if (this.currentGain == volume) {
            return true;
        }
        if (this.currentGain < volume) {
            this.currentGain += Mth.clamp(this.currentGain, 5.0E-4f, 0.005f);
            if (this.currentGain > volume) {
                this.currentGain = volume;
            }
        } else {
            this.currentGain = 0.03f * volume + 0.97f * this.currentGain;
            if (Math.abs(this.currentGain - volume) < 1.0E-4f || this.currentGain < volume) {
                this.currentGain = volume;
            }
        }
        this.currentGain = Mth.clamp(this.currentGain, 0.0f, 1.0f);
        if (this.currentGain <= 1.0E-4f) {
            this.stopPlaying();
            return false;
        }
        this.minecraft.getSoundManager().updateCategoryVolume(SoundSource.MUSIC, this.currentGain);
        return true;
    }

    public boolean isPlayingMusic(Music music) {
        if (this.currentMusic == null) {
            return false;
        }
        return music.sound().value().location().equals(this.currentMusic.getIdentifier());
    }

    public @Nullable String getCurrentMusicTranslationKey() {
        Sound sound;
        if (this.currentMusic != null && (sound = this.currentMusic.getSound()) != null) {
            return sound.getLocation().toShortLanguageKey();
        }
        return null;
    }

    public void setMinutesBetweenSongs(MusicFrequency musicFrequency) {
        this.gameMusicFrequency = musicFrequency;
        this.nextSongDelay = this.gameMusicFrequency.getNextSongDelay(this.minecraft.getSituationalMusic(), this.random);
    }

    @Environment(value=EnvType.CLIENT)
    public static enum MusicFrequency implements StringRepresentable
    {
        DEFAULT("DEFAULT", "options.music_frequency.default", 20),
        FREQUENT("FREQUENT", "options.music_frequency.frequent", 10),
        CONSTANT("CONSTANT", "options.music_frequency.constant", 0);

        public static final Codec<MusicFrequency> CODEC;
        private final String name;
        private final int maxFrequency;
        private final Component caption;

        private MusicFrequency(String name, String translationKey, int maxFrequencyMinutes) {
            this.name = name;
            this.maxFrequency = maxFrequencyMinutes * 1200;
            this.caption = Component.translatable(translationKey);
        }

        private int getNextSongDelay(@Nullable Music music, RandomSource random) {
            if (music == null) {
                return this.maxFrequency;
            }
            if (this == CONSTANT) {
                return 100;
            }
            int minFrequency = Math.min(music.minDelay(), this.maxFrequency);
            int maxFrequency = Math.min(music.maxDelay(), this.maxFrequency);
            return Mth.nextInt(random, minFrequency, maxFrequency);
        }

        public Component caption() {
            return this.caption;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(MusicFrequency::values);
        }
    }
}

