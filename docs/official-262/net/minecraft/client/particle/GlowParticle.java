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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class GlowParticle
extends SingleQuadParticle {
    private final SpriteSet sprites;

    private GlowParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, SpriteSet sprites) {
        super(level, x, y, z, xa, ya, za, sprites.first());
        this.friction = 0.96f;
        this.speedUpWhenYMotionIsBlocked = true;
        this.sprites = sprites;
        this.quadSize *= 0.75f;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public int getLightCoords(float a) {
        return LightCoordsUtil.addSmoothBlockEmission(super.getLightCoords(a), ((float)this.age + a) / (float)this.lifetime);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    @Environment(value=EnvType.CLIENT)
    public static class ScrapeProvider
    implements ParticleProvider<SimpleParticleType> {
        private static final double SPEED_FACTOR = 0.01;
        private final SpriteSet sprite;

        public ScrapeProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            GlowParticle glowParticle = new GlowParticle(level, x, y, z, 0.0, 0.0, 0.0, this.sprite);
            if (random.nextBoolean()) {
                glowParticle.setColor(0.29f, 0.58f, 0.51f);
            } else {
                glowParticle.setColor(0.43f, 0.77f, 0.62f);
            }
            glowParticle.setParticleSpeed(xAux * 0.01, yAux * 0.01, zAux * 0.01);
            int minLifespan = 10;
            int maxLifespan = 40;
            glowParticle.setLifetime(random.nextInt(30) + 10);
            return glowParticle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class ElectricSparkProvider
    implements ParticleProvider<SimpleParticleType> {
        private static final double SPEED_FACTOR = 0.25;
        private final SpriteSet sprite;

        public ElectricSparkProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            GlowParticle glowParticle = new GlowParticle(level, x, y, z, 0.0, 0.0, 0.0, this.sprite);
            glowParticle.setColor(1.0f, 0.9f, 1.0f);
            glowParticle.setParticleSpeed(xAux * 0.25, yAux * 0.25, zAux * 0.25);
            int minLifespan = 2;
            int maxLifespan = 4;
            glowParticle.setLifetime(random.nextInt(2) + 2);
            return glowParticle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class WaxOffProvider
    implements ParticleProvider<SimpleParticleType> {
        private static final double SPEED_FACTOR = 0.01;
        private final SpriteSet sprite;

        public WaxOffProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            GlowParticle glowParticle = new GlowParticle(level, x, y, z, 0.0, 0.0, 0.0, this.sprite);
            glowParticle.setColor(1.0f, 0.9f, 1.0f);
            glowParticle.setParticleSpeed(xAux * 0.01 / 2.0, yAux * 0.01, zAux * 0.01 / 2.0);
            int minLifespan = 10;
            int maxLifespan = 40;
            glowParticle.setLifetime(random.nextInt(30) + 10);
            return glowParticle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class WaxOnProvider
    implements ParticleProvider<SimpleParticleType> {
        private static final double SPEED_FACTOR = 0.01;
        private final SpriteSet sprite;

        public WaxOnProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            GlowParticle glowParticle = new GlowParticle(level, x, y, z, 0.0, 0.0, 0.0, this.sprite);
            glowParticle.setColor(0.91f, 0.55f, 0.08f);
            glowParticle.setParticleSpeed(xAux * 0.01 / 2.0, yAux * 0.01, zAux * 0.01 / 2.0);
            int minLifespan = 10;
            int maxLifespan = 40;
            glowParticle.setLifetime(random.nextInt(30) + 10);
            return glowParticle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class GlowSquidProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public GlowSquidProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            GlowParticle glowParticle = new GlowParticle(level, x, y, z, 0.5 - random.nextDouble(), yAux, 0.5 - random.nextDouble(), this.sprite);
            if (random.nextBoolean()) {
                glowParticle.setColor(0.6f, 1.0f, 0.8f);
            } else {
                glowParticle.setColor(0.08f, 0.4f, 0.4f);
            }
            glowParticle.yd *= (double)0.2f;
            if (xAux == 0.0 && zAux == 0.0) {
                glowParticle.xd *= (double)0.1f;
                glowParticle.zd *= (double)0.1f;
            }
            glowParticle.setLifetime((int)(8.0 / (random.nextDouble() * 0.8 + 0.2)));
            return glowParticle;
        }
    }
}

