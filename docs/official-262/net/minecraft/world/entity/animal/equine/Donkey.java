/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.equine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class Donkey
extends AbstractChestedHorse {
    public Donkey(EntityType<? extends Donkey> type, Level level) {
        super((EntityType<? extends AbstractChestedHorse>)type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.DONKEY_AMBIENT;
    }

    @Override
    protected SoundEvent getAngrySound() {
        return SoundEvents.DONKEY_ANGRY;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DONKEY_DEATH;
    }

    @Override
    protected SoundEvent getEatingSound() {
        return SoundEvents.DONKEY_EAT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DONKEY_HURT;
    }

    @Override
    public boolean canMate(Animal partner) {
        if (partner == this) {
            return false;
        }
        if (partner instanceof Donkey || partner instanceof Horse) {
            return this.canParent() && ((AbstractHorse)partner).canParent();
        }
        return false;
    }

    @Override
    protected void playJumpSound() {
        this.playSound(SoundEvents.DONKEY_JUMP, 0.4f, 1.0f);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        EntityType<AbstractChestedHorse> babyType = partner instanceof Horse ? EntityTypes.MULE : EntityTypes.DONKEY;
        AbstractHorse baby = babyType.create((Level)level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            this.setOffspringAttributes(partner, baby);
        }
        return baby;
    }
}

