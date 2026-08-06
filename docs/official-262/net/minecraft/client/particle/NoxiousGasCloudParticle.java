/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.PotentSulfurBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class NoxiousGasCloudParticle
extends NoRenderParticle {
    private static final int PARTICLE_TICKS = 2;

    protected NoxiousGasCloudParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 20;
    }

    @Override
    public void tick() {
        Vec3 particlePos;
        super.tick();
        if (this.age % 2 != 0) {
            return;
        }
        BlockPos sourceBlock = BlockPos.containing(this.x, this.y, this.z);
        if (PotentSulfurBlockEntity.canBeReachedByNoxiousGas(this.level, sourceBlock, particlePos = NoxiousGasCloudParticle.pickRandomParticleSpawnPoint(this.level, sourceBlock))) {
            NoxiousGasCloudParticle.spawnNoxiousGasParticle(this.level, particlePos);
        }
    }

    private static Vec3 pickRandomParticleSpawnPoint(Level level, BlockPos centerBlock) {
        RandomSource random = level.getRandom();
        Vec3 horizontalDirection = new Vec3(random.nextFloat() - 0.5f, 0.0, random.nextFloat() - 0.5f).normalize();
        float distance = random.nextFloat() * 3.0f;
        return Vec3.atCenterOf(centerBlock).add(horizontalDirection.scale(distance)).subtract(0.0, 0.25, 0.0);
    }

    private static void spawnNoxiousGasParticle(Level level, Vec3 pos) {
        level.addAlwaysVisibleParticle(ParticleTypes.NOXIOUS_GAS, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<SimpleParticleType> {
        @Override
        public @Nullable Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new NoxiousGasCloudParticle(level, x, y, z);
        }
    }
}

