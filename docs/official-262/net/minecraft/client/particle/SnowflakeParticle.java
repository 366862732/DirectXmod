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
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class SnowflakeParticle
extends SingleQuadParticle {
    private final SpriteSet sprites;

    protected SnowflakeParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.gravity = 0.225f;
        this.friction = 1.0f;
        this.sprites = sprites;
        this.xd = xa + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 0.05f);
        this.yd = ya + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 0.05f);
        this.zd = za + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 0.05f);
        this.quadSize = 0.1f * (this.random.nextFloat() * this.random.nextFloat() * 1.0f + 1.0f);
        this.lifetime = (int)(16.0 / ((double)this.random.nextFloat() * 0.8 + 0.2)) + 2;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        this.xd *= (double)0.95f;
        this.yd *= (double)0.9f;
        this.zd *= (double)0.95f;
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
            SnowflakeParticle snowflakeParticle = new SnowflakeParticle(level, x, y, z, xAux, yAux, zAux, this.sprites);
            snowflakeParticle.setColor(0.923f, 0.964f, 0.999f);
            return snowflakeParticle;
        }
    }
}

