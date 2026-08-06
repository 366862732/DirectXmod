/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DragonLandingPhase
extends AbstractDragonPhaseInstance {
    private @Nullable Vec3 targetLocation;

    public DragonLandingPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    public void doClientTick() {
        Vec3 look = this.dragon.getHeadLookVector(1.0f).normalize();
        look.yRot(-0.7853982f);
        double particleX = this.dragon.head.getX();
        double particleY = this.dragon.head.getY(0.5);
        double particleZ = this.dragon.head.getZ();
        for (int i = 0; i < 8; ++i) {
            RandomSource random = this.dragon.getRandom();
            double px = particleX + random.nextGaussian() / 2.0;
            double py = particleY + random.nextGaussian() / 2.0;
            double pz = particleZ + random.nextGaussian() / 2.0;
            Vec3 movement = this.dragon.getDeltaMovement();
            this.dragon.level().addParticle(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0f), px, py, pz, -look.x * (double)0.08f + movement.x, -look.y * (double)0.3f + movement.y, -look.z * (double)0.08f + movement.z);
            look.yRot(0.19634955f);
        }
    }

    @Override
    public void doServerTick(ServerLevel level) {
        if (this.targetLocation == null) {
            this.targetLocation = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(this.dragon.getFightOrigin())));
        }
        if (this.targetLocation.distanceToSqr(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ()) < 1.0) {
            this.dragon.getPhaseManager().getPhase(EnderDragonPhase.SITTING_FLAMING).resetFlameCount();
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
        }
    }

    @Override
    public float getFlySpeed() {
        return 1.5f;
    }

    @Override
    public float getTurnSpeed() {
        float rotSpeed = (float)this.dragon.getDeltaMovement().horizontalDistance() + 1.0f;
        float dist = Math.min(rotSpeed, 40.0f);
        return dist / rotSpeed;
    }

    @Override
    public void begin() {
        this.targetLocation = null;
    }

    @Override
    public @Nullable Vec3 getFlyTargetLocation() {
        return this.targetLocation;
    }

    public EnderDragonPhase<DragonLandingPhase> getPhase() {
        return EnderDragonPhase.LANDING;
    }
}

