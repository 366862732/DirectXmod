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
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class DustPlumeParticle
extends BaseAshSmokeParticle {
    private static final int COLOR_RGB24 = 12235202;

    protected DustPlumeParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, float scale, SpriteSet sprites) {
        super(level, x, y, z, 0.7f, 0.6f, 0.7f, xa, ya + (double)0.15f, za, scale, sprites, 0.5f, 7, 0.5f, false);
        float colorShift = this.random.nextFloat() * 0.2f;
        this.rCol = (float)ARGB.red(12235202) / 255.0f - colorShift;
        this.gCol = (float)ARGB.green(12235202) / 255.0f - colorShift;
        this.bCol = (float)ARGB.blue(12235202) / 255.0f - colorShift;
    }

    @Override
    public void tick() {
        this.gravity = 0.88f * this.gravity;
        this.friction = 0.92f * this.friction;
        super.tick();
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
            return new DustPlumeParticle(level, x, y, z, xAux, yAux, zAux, 1.0f, this.sprites);
        }
    }
}

