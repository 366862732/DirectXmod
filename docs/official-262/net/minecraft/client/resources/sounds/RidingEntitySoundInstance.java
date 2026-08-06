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
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@Environment(value=EnvType.CLIENT)
public class RidingEntitySoundInstance
extends AbstractTickableSoundInstance {
    private final Player player;
    private final Entity entity;
    private final boolean underwaterSound;
    private final float volumeMin;
    private final float volumeMax;
    private final float volumeAmplifier;

    public RidingEntitySoundInstance(Player player, Entity entity, boolean underwaterSound, SoundEvent soundEvent, SoundSource soundSource, float volumeMin, float volumeMax, float volumeAmplifier) {
        super(soundEvent, soundSource, SoundInstance.createUnseededRandom());
        this.player = player;
        this.entity = entity;
        this.underwaterSound = underwaterSound;
        this.volumeMin = volumeMin;
        this.volumeMax = volumeMax;
        this.volumeAmplifier = volumeAmplifier;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.looping = true;
        this.delay = 0;
        this.volume = volumeMin;
    }

    @Override
    public boolean canPlaySound() {
        return !this.entity.isSilent();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    protected boolean shouldNotPlayUnderwaterSound() {
        return this.underwaterSound != this.entity.isUnderWater();
    }

    protected float getEntitySpeed() {
        return (float)this.entity.getDeltaMovement().length();
    }

    protected boolean shoudlPlaySound() {
        return true;
    }

    @Override
    public void tick() {
        if (this.entity.isRemoved() || !this.player.isPassenger() || this.player.getVehicle() != this.entity) {
            this.stop();
            return;
        }
        if (this.shouldNotPlayUnderwaterSound()) {
            this.volume = this.volumeMin;
            return;
        }
        float speed = this.getEntitySpeed();
        this.volume = speed >= 0.01f && this.shoudlPlaySound() ? this.volumeAmplifier * Mth.clampedLerp(speed, this.volumeMin, this.volumeMax) : this.volumeMin;
    }
}

