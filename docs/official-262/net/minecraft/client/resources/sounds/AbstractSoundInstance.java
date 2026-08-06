/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.resources.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractSoundInstance
implements SoundInstance {
    protected @Nullable Sound sound;
    protected final SoundSource source;
    protected final Identifier identifier;
    protected float volume = 1.0f;
    protected float pitch = 1.0f;
    protected double x;
    protected double y;
    protected double z;
    protected boolean looping;
    protected int delay;
    protected SoundInstance.Attenuation attenuation = SoundInstance.Attenuation.LINEAR;
    protected boolean relative;
    protected final RandomSource random;

    protected AbstractSoundInstance(SoundEvent event, SoundSource source, RandomSource random) {
        this(event.location(), source, random);
    }

    protected AbstractSoundInstance(Identifier identifier, SoundSource source, RandomSource random) {
        this.identifier = identifier;
        this.source = source;
        this.random = random;
    }

    @Override
    public Identifier getIdentifier() {
        return this.identifier;
    }

    @Override
    public @Nullable WeighedSoundEvents resolve(SoundManager soundManager) {
        if (this.identifier.equals(SoundManager.INTENTIONALLY_EMPTY_SOUND_LOCATION)) {
            this.sound = SoundManager.INTENTIONALLY_EMPTY_SOUND;
            return SoundManager.INTENTIONALLY_EMPTY_SOUND_EVENT;
        }
        WeighedSoundEvents soundEvent = soundManager.getSoundEvent(this.identifier);
        this.sound = soundEvent == null ? SoundManager.EMPTY_SOUND : soundEvent.getSound(this.random);
        return soundEvent;
    }

    @Override
    public @Nullable Sound getSound() {
        return this.sound;
    }

    @Override
    public SoundSource getSource() {
        return this.source;
    }

    @Override
    public boolean isLooping() {
        return this.looping;
    }

    @Override
    public int getDelay() {
        return this.delay;
    }

    @Override
    public float getVolume() {
        return this.volume * this.sound.getVolume().sample(this.random);
    }

    @Override
    public float getPitch() {
        return this.pitch * this.sound.getPitch().sample(this.random);
    }

    @Override
    public double getX() {
        return this.x;
    }

    @Override
    public double getY() {
        return this.y;
    }

    @Override
    public double getZ() {
        return this.z;
    }

    @Override
    public SoundInstance.Attenuation getAttenuation() {
        return this.attenuation;
    }

    @Override
    public boolean isRelative() {
        return this.relative;
    }

    public String toString() {
        return "SoundInstance[" + String.valueOf(this.identifier) + "]";
    }
}

