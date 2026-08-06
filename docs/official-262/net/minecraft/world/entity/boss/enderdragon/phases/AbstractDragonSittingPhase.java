/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;

public abstract class AbstractDragonSittingPhase
extends AbstractDragonPhaseInstance {
    public AbstractDragonSittingPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    public boolean isSitting() {
        return true;
    }

    @Override
    public float onHurt(DamageSource source, float damage) {
        if (source.getDirectEntity() instanceof AbstractArrow || source.getDirectEntity() instanceof WindCharge) {
            source.getDirectEntity().igniteForSeconds(1.0f);
            return 0.0f;
        }
        return super.onHurt(source, damage);
    }
}

