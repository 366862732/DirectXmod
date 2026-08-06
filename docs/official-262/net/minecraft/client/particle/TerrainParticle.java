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
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class TerrainParticle
extends SingleQuadParticle {
    private final SingleQuadParticle.Layer layer;
    private final BlockPos pos;
    private final float uo;
    private final float vo;

    public TerrainParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, BlockState blockState) {
        this(level, x, y, z, xa, ya, za, blockState, BlockPos.containing(x, y, z));
    }

    public TerrainParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, BlockState blockState, BlockPos pos) {
        super(level, x, y, z, xa, ya, za, Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(blockState).sprite());
        this.pos = pos;
        this.gravity = 1.0f;
        this.rCol = 0.6f;
        this.gCol = 0.6f;
        this.bCol = 0.6f;
        BlockTintSource tintSource = Minecraft.getInstance().getBlockColors().getTintSource(blockState, 0);
        if (tintSource != null) {
            int col = tintSource.colorAsTerrainParticle(blockState, level, pos);
            this.rCol *= (float)(col >> 16 & 0xFF) / 255.0f;
            this.gCol *= (float)(col >> 8 & 0xFF) / 255.0f;
            this.bCol *= (float)(col & 0xFF) / 255.0f;
        }
        this.quadSize /= 2.0f;
        this.uo = this.random.nextFloat() * 3.0f;
        this.vo = this.random.nextFloat() * 3.0f;
        this.layer = SingleQuadParticle.Layer.bySprite(this.sprite);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return this.layer;
    }

    @Override
    protected float getU0() {
        return this.sprite.getU((this.uo + 1.0f) / 4.0f);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU(this.uo / 4.0f);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.vo / 4.0f);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.vo + 1.0f) / 4.0f);
    }

    private static @Nullable TerrainParticle createTerrainParticle(BlockParticleOption options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux) {
        BlockState state = options.getState();
        if (state.isAir() || state.is(Blocks.MOVING_PISTON) || !state.shouldSpawnTerrainParticles()) {
            return null;
        }
        return new TerrainParticle(level, x, y, z, xAux, yAux, zAux, state);
    }

    @Environment(value=EnvType.CLIENT)
    public static class CrumblingProvider
    implements ParticleProvider<BlockParticleOption> {
        @Override
        public @Nullable Particle createParticle(BlockParticleOption options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            TerrainParticle particle = TerrainParticle.createTerrainParticle(options, level, x, y, z, xAux, yAux, zAux);
            if (particle != null) {
                particle.setParticleSpeed(0.0, 0.0, 0.0);
                particle.setLifetime(random.nextInt(10) + 1);
            }
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class DustPillarProvider
    implements ParticleProvider<BlockParticleOption> {
        @Override
        public @Nullable Particle createParticle(BlockParticleOption options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            TerrainParticle particle = TerrainParticle.createTerrainParticle(options, level, x, y, z, xAux, yAux, zAux);
            if (particle != null) {
                particle.setParticleSpeed(random.nextGaussian() / 30.0, yAux + random.nextGaussian() / 2.0, random.nextGaussian() / 30.0);
                particle.setLifetime(random.nextInt(20) + 20);
            }
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<BlockParticleOption> {
        @Override
        public @Nullable Particle createParticle(BlockParticleOption options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return TerrainParticle.createTerrainParticle(options, level, x, y, z, xAux, yAux, zAux);
        }
    }
}

