/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class ClientExplosionTracker {
    private static final int MAX_PARTICLES_PER_TICK = 512;
    private final List<ExplosionInfo> explosions = new ArrayList<ExplosionInfo>();

    public void track(Vec3 center, float radius, int blockCount, WeightedList<ExplosionParticleInfo> blockParticles) {
        if (!blockParticles.isEmpty()) {
            this.explosions.add(new ExplosionInfo(center, radius, blockCount, blockParticles));
        }
    }

    public void tick(ClientLevel level) {
        if (Minecraft.getInstance().options.particles().get() != ParticleStatus.ALL) {
            this.explosions.clear();
            return;
        }
        int totalBlocks = WeightedRandom.getTotalWeight(this.explosions, ExplosionInfo::blockCount);
        int totalParticles = Math.min(totalBlocks, 512);
        for (int i = 0; i < totalParticles; ++i) {
            WeightedRandom.getRandomItem(level.getRandom(), this.explosions, totalBlocks, ExplosionInfo::blockCount).ifPresent(info -> this.addParticle(level, (ExplosionInfo)info));
        }
        this.explosions.clear();
    }

    private void addParticle(ClientLevel level, ExplosionInfo explosion) {
        float radius;
        Vec3 directionFromCenter;
        Vec3 localPos;
        RandomSource random = level.getRandom();
        Vec3 center = explosion.center();
        Vec3 pos = center.add(localPos = (directionFromCenter = new Vec3(random.nextFloat() * 2.0f - 1.0f, random.nextFloat() * 2.0f - 1.0f, random.nextFloat() * 2.0f - 1.0f).normalize()).scale(radius = (float)Math.cbrt(random.nextFloat()) * explosion.radius()));
        if (!level.getBlockState(BlockPos.containing(pos)).isAir()) {
            return;
        }
        float speed = 0.5f / (radius / explosion.radius() + 0.1f) * random.nextFloat() * random.nextFloat() + 0.3f;
        ExplosionParticleInfo info = explosion.blockParticles.getRandomOrThrow(random);
        Vec3 particlePos = center.add(localPos.scale(info.scaling()));
        Vec3 particleVelocity = directionFromCenter.scale(speed * info.speed());
        level.addParticle(info.particle(), particlePos.x(), particlePos.y(), particlePos.z(), particleVelocity.x(), particleVelocity.y(), particleVelocity.z());
    }

    @Environment(value=EnvType.CLIENT)
    private record ExplosionInfo(Vec3 center, float radius, int blockCount, WeightedList<ExplosionParticleInfo> blockParticles) {
    }
}

