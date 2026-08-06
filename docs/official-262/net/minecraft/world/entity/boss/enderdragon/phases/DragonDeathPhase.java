/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DragonDeathPhase
extends AbstractDragonPhaseInstance {
    private @Nullable Vec3 targetLocation;
    private int time;

    public DragonDeathPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    public void doClientTick() {
        if (this.time++ % 10 == 0) {
            float xo = (this.dragon.getRandom().nextFloat() - 0.5f) * 8.0f;
            float yo = (this.dragon.getRandom().nextFloat() - 0.5f) * 4.0f;
            float zo = (this.dragon.getRandom().nextFloat() - 0.5f) * 8.0f;
            this.dragon.level().addParticle(ParticleTypes.EXPLOSION_EMITTER, this.dragon.getX() + (double)xo, this.dragon.getY() + 2.0 + (double)yo, this.dragon.getZ() + (double)zo, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void doServerTick(ServerLevel level) {
        double distToTarget;
        ++this.time;
        if (this.targetLocation == null) {
            BlockPos egg = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, EndPodiumFeature.getLocation(this.dragon.getFightOrigin()));
            this.targetLocation = Vec3.atBottomCenterOf(egg);
        }
        if ((distToTarget = this.targetLocation.distanceToSqr(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ())) < 100.0 || distToTarget > 22500.0 || this.dragon.horizontalCollision || this.dragon.verticalCollision) {
            this.dragon.setHealth(0.0f);
        } else {
            this.dragon.setHealth(1.0f);
        }
    }

    @Override
    public void begin() {
        this.targetLocation = null;
        this.time = 0;
    }

    @Override
    public float getFlySpeed() {
        return 3.0f;
    }

    @Override
    public @Nullable Vec3 getFlyTargetLocation() {
        return this.targetLocation;
    }

    public EnderDragonPhase<DragonDeathPhase> getPhase() {
        return EnderDragonPhase.DYING;
    }
}

