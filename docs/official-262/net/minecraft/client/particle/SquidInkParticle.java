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
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class SquidInkParticle
extends SimpleAnimatedParticle {
    private SquidInkParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, int color, SpriteSet sprites) {
        super(level, x, y, z, sprites, 0.0f);
        this.friction = 0.92f;
        this.quadSize = 0.5f;
        this.setAlpha(1.0f);
        this.setColor(ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color));
        this.lifetime = (int)(this.quadSize * 12.0f / (this.random.nextFloat() * 0.8f + 0.2f));
        this.setSpriteFromAge(sprites);
        this.hasPhysics = false;
        this.xd = xa;
        this.yd = ya;
        this.zd = za;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
            if (this.age > this.lifetime / 2) {
                this.setAlpha(1.0f - ((float)this.age - (float)(this.lifetime / 2)) / (float)this.lifetime);
            }
            if (this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isAir()) {
                this.yd -= (double)0.0074f;
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class GlowInkProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public GlowInkProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new SquidInkParticle(level, x, y, z, xAux, yAux, zAux, ARGB.colorFromFloat(1.0f, 0.2f, 0.8f, 0.6f), this.sprites);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new SquidInkParticle(level, x, y, z, xAux, yAux, zAux, -16777216, this.sprites);
        }
    }
}

