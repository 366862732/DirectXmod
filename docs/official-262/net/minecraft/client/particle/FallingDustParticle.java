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
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class FallingDustParticle
extends SingleQuadParticle {
    private final float rotSpeed;
    private final SpriteSet sprites;

    private FallingDustParticle(ClientLevel level, double x, double y, double z, float r, float g, float b, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        float scale = 0.9f;
        this.quadSize *= 0.67499995f;
        int baseLifetime = (int)(32.0 / ((double)this.random.nextFloat() * 0.8 + 0.2));
        this.lifetime = (int)Math.max((float)baseLifetime * 0.9f, 1.0f);
        this.setSpriteFromAge(sprites);
        this.rotSpeed = (this.random.nextFloat() - 0.5f) * 0.1f;
        this.roll = this.random.nextFloat() * ((float)Math.PI * 2);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public float getQuadSize(float a) {
        return this.quadSize * Mth.clamp(((float)this.age + a) / (float)this.lifetime * 32.0f, 0.0f, 1.0f);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.setSpriteFromAge(this.sprites);
        this.oRoll = this.roll;
        this.roll += (float)Math.PI * this.rotSpeed * 2.0f;
        if (this.onGround) {
            this.roll = 0.0f;
            this.oRoll = 0.0f;
        }
        this.move(this.xd, this.yd, this.zd);
        this.yd -= (double)0.003f;
        this.yd = Math.max(this.yd, (double)-0.14f);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<BlockParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public @Nullable Particle createParticle(BlockParticleOption options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            int tintColor;
            BlockState blockState = options.getState();
            if (!blockState.isAir() && blockState.getRenderShape() == RenderShape.INVISIBLE) {
                return null;
            }
            BlockPos pos = BlockPos.containing(x, y, z);
            Block block = blockState.getBlock();
            if (block instanceof FallingBlock) {
                FallingBlock fallingBlock = (FallingBlock)block;
                tintColor = fallingBlock.getDustColor(blockState, level, pos);
            } else {
                BlockTintSource tintSource = Minecraft.getInstance().getBlockColors().getTintSource(blockState, 0);
                tintColor = tintSource != null ? tintSource.colorAsTerrainParticle(blockState, level, pos) : blockState.getMapColor((BlockGetter)level, (BlockPos)pos).col;
            }
            float r = (float)(tintColor >> 16 & 0xFF) / 255.0f;
            float g = (float)(tintColor >> 8 & 0xFF) / 255.0f;
            float b = (float)(tintColor & 0xFF) / 255.0f;
            return new FallingDustParticle(level, x, y, z, r, g, b, this.sprite);
        }
    }
}

