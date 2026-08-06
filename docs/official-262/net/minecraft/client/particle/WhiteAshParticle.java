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
public class WhiteAshParticle
extends BaseAshSmokeParticle {
    private static final int COLOR_RGB24 = 12235202;

    protected WhiteAshParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, float scale, SpriteSet sprites) {
        super(level, x, y, z, 0.1f, -0.1f, 0.1f, xa, ya, za, scale, sprites, 0.0f, 20, 0.0125f, false);
        this.rCol = (float)ARGB.red(12235202) / 255.0f;
        this.gCol = (float)ARGB.green(12235202) / 255.0f;
        this.bCol = (float)ARGB.blue(12235202) / 255.0f;
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
            double xa = (double)random.nextFloat() * -1.9 * (double)random.nextFloat() * 0.1;
            double ya = (double)random.nextFloat() * -0.5 * (double)random.nextFloat() * 0.1 * 5.0;
            double za = (double)random.nextFloat() * -1.9 * (double)random.nextFloat() * 0.1;
            return new WhiteAshParticle(level, x, y, z, xa, ya, za, 1.0f, this.sprites);
        }
    }
}

