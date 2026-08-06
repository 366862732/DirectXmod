/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class SimpleSoundInstance
extends AbstractSoundInstance {
    public SimpleSoundInstance(SoundEvent sound, SoundSource source, float volume, float pitch, RandomSource random, BlockPos pos) {
        this(sound, source, volume, pitch, random, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
    }

    public static SimpleSoundInstance forUI(SoundEvent sound, float pitch) {
        return SimpleSoundInstance.forUI(sound, pitch, 0.25f);
    }

    public static SimpleSoundInstance forUI(Holder<SoundEvent> sound, float pitch) {
        return SimpleSoundInstance.forUI(sound.value(), pitch);
    }

    public static SimpleSoundInstance forUI(SoundEvent sound, float pitch, float volume) {
        return new SimpleSoundInstance(sound.location(), SoundSource.UI, volume, pitch, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
    }

    public static SimpleSoundInstance forMusic(SoundEvent sound) {
        return new SimpleSoundInstance(sound.location(), SoundSource.MUSIC, 1.0f, 1.0f, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
    }

    public static SimpleSoundInstance forJukeboxSong(SoundEvent sound, Vec3 pos) {
        return new SimpleSoundInstance(sound, SoundSource.RECORDS, 4.0f, 1.0f, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.LINEAR, pos.x, pos.y, pos.z);
    }

    public static SimpleSoundInstance forLocalAmbience(SoundEvent sound, float pitch, float volume) {
        return new SimpleSoundInstance(sound.location(), SoundSource.AMBIENT, volume, pitch, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
    }

    public static SimpleSoundInstance forAmbientAddition(SoundEvent sound) {
        return SimpleSoundInstance.forLocalAmbience(sound, 1.0f, 1.0f);
    }

    public static SimpleSoundInstance forAmbientMood(SoundEvent sound, RandomSource random, double x, double y, double z) {
        return new SimpleSoundInstance(sound, SoundSource.AMBIENT, 1.0f, 1.0f, random, false, 0, SoundInstance.Attenuation.LINEAR, x, y, z);
    }

    public SimpleSoundInstance(SoundEvent sound, SoundSource source, float volume, float pitch, RandomSource random, double x, double y, double z) {
        this(sound, source, volume, pitch, random, false, 0, SoundInstance.Attenuation.LINEAR, x, y, z);
    }

    private SimpleSoundInstance(SoundEvent sound, SoundSource source, float volume, float pitch, RandomSource random, boolean looping, int delay, SoundInstance.Attenuation attenuation, double x, double y, double z) {
        this(sound.location(), source, volume, pitch, random, looping, delay, attenuation, x, y, z, false);
    }

    public SimpleSoundInstance(Identifier location, SoundSource source, float volume, float pitch, RandomSource random, boolean looping, int delay, SoundInstance.Attenuation attenuation, double x, double y, double z, boolean relative) {
        super(location, source, random);
        this.volume = volume;
        this.pitch = pitch;
        this.x = x;
        this.y = y;
        this.z = z;
        this.looping = looping;
        this.delay = delay;
        this.attenuation = attenuation;
        this.relative = relative;
    }
}

