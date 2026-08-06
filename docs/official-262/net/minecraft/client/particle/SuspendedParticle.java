/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.particle;

import java.util.Objects;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class SuspendedParticle
extends SingleQuadParticle {
    private SuspendedParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y - 0.125, z, sprite);
        this.setSize(0.01f, 0.01f);
        this.quadSize *= this.random.nextFloat() * 0.6f + 0.2f;
        this.lifetime = (int)(16.0 / ((double)this.random.nextFloat() * 0.8 + 0.2));
        this.hasPhysics = false;
        this.friction = 1.0f;
        this.gravity = 0.0f;
    }

    private SuspendedParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
        super(level, x, y - 0.125, z, xd, yd, zd, sprite);
        this.setSize(0.01f, 0.01f);
        this.quadSize *= this.random.nextFloat() * 0.6f + 0.6f;
        this.lifetime = (int)(16.0 / ((double)this.random.nextFloat() * 0.8 + 0.2));
        this.hasPhysics = false;
        this.friction = 1.0f;
        this.gravity = 0.0f;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Environment(value=EnvType.CLIENT)
    public static class WarpedSporeProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public WarpedSporeProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            double ya = (double)random.nextFloat() * -1.9 * (double)random.nextFloat() * 0.1;
            SuspendedParticle particle = new SuspendedParticle(level, x, y, z, 0.0, ya, 0.0, this.sprite.get(random));
            particle.setColor(0.1f, 0.1f, 0.3f);
            particle.setSize(0.001f, 0.001f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class CrimsonSporeProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public CrimsonSporeProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            double xa = random.nextGaussian() * (double)1.0E-6f;
            double ya = random.nextGaussian() * (double)1.0E-4f;
            double za = random.nextGaussian() * (double)1.0E-6f;
            SuspendedParticle particle = new SuspendedParticle(level, x, y, z, xa, ya, za, this.sprite.get(random));
            particle.setColor(0.9f, 0.4f, 0.5f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class SporeBlossomAirProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public SporeBlossomAirProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            SuspendedParticle particle = new SuspendedParticle(this, level, x, y, z, 0.0, -0.8f, 0.0, this.sprite.get(random)){
                {
                    Objects.requireNonNull(this$0);
                    super(level, x, y, z, xd, yd, zd, sprite);
                }

                @Override
                public Optional<ParticleLimit> getParticleLimit() {
                    return Optional.of(ParticleLimit.SPORE_BLOSSOM);
                }
            };
            particle.lifetime = Mth.randomBetweenInclusive(random, 500, 1000);
            particle.gravity = 0.01f;
            particle.setColor(0.32f, 0.5f, 0.22f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class UnderwaterProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public UnderwaterProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            SuspendedParticle particle = new SuspendedParticle(level, x, y, z, this.sprite.get(random));
            particle.setColor(0.4f, 0.4f, 0.7f);
            return particle;
        }
    }
}

