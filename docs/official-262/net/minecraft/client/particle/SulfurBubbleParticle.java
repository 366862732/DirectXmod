/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluids;

@Environment(value=EnvType.CLIENT)
public class SulfurBubbleParticle
extends SingleQuadParticle {
    private static final float SIZE_START = 0.02f;
    private static final float SIZE_END = 0.15f;
    private static final float UPWARDS_SPEED = 0.04f;
    private static final float HORIZONTAL_WIGGLING = 0.003f;
    private final double yStart;
    private final double yEnd;
    private final float sizeStart;
    private double yPrev;

    private SulfurBubbleParticle(ClientLevel level, double x, double y, double z, double xa, double za, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.gravity = -0.04f;
        this.friction = 0.85f;
        this.setSize(0.02f, 0.02f);
        this.xd = xa * (double)0.2f + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 0.02f);
        this.zd = za * (double)0.2f + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 0.02f);
        this.quadSize = this.sizeStart = 0.02f + 0.02f * this.random.nextFloat();
        this.lifetime = Integer.MAX_VALUE;
        this.yStart = this.yo;
        this.yEnd = this.yo + 4.0 - 1.0;
        this.yPrev = y;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed && !this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).isSourceOfType(Fluids.WATER)) {
            this.remove();
        }
        if (!this.removed && this.y >= this.yEnd) {
            this.remove();
        }
        if (!this.removed && this.y <= this.yPrev) {
            this.remove();
        }
        this.xd += this.randomHorizontalWiggling();
        this.zd += this.randomHorizontalWiggling();
        this.move(this.xd, 0.0, this.zd);
        float travelProgress = (float)((this.y - this.yStart) / (this.yEnd - this.yStart));
        this.quadSize = this.sizeStart + travelProgress * (0.15f - this.sizeStart);
        this.yPrev = this.y;
    }

    private double randomHorizontalWiggling() {
        return (double)(this.random.nextFloat() * 0.003f * (float)(this.random.nextBoolean() ? 1 : -1)) * 0.5;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new SulfurBubbleParticle(level, x, y, z, xAux, yAux, this.sprite.get(random));
        }
    }
}

