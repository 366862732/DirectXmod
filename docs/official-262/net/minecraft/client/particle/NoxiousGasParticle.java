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
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class NoxiousGasParticle
extends BaseAshSmokeParticle {
    private final float fadeOutStartingPoint;

    protected NoxiousGasParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, float scale, SpriteSet sprites) {
        super(level, x, y, z, 0.1f, 0.1f, 0.1f, xa, ya, za, scale, sprites, 0.3f, 5, -0.02f, true);
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        this.lifetime = (int)(6.0 / ((double)this.random.nextFloat() * 0.5 + 0.5) * (double)scale);
        this.fadeOutStartingPoint = (float)this.lifetime / 2.0f;
    }

    @Override
    public void tick() {
        super.tick();
        if ((float)this.age > this.fadeOutStartingPoint) {
            float framesSinceFadeOutStart = (float)this.age - this.fadeOutStartingPoint;
            this.setAlpha(((float)this.lifetime - framesSinceFadeOutStart) / (float)this.lifetime);
        }
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
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
            return new NoxiousGasParticle(level, x, y, z, xAux, yAux, zAux, 3.0f, this.sprites);
        }
    }
}

