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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class FireflyParticle
extends SingleQuadParticle {
    private static final float PARTICLE_FADE_OUT_LIGHT_TIME = 0.3f;
    private static final float PARTICLE_FADE_IN_LIGHT_TIME = 0.1f;
    private static final float PARTICLE_FADE_OUT_ALPHA_TIME = 0.5f;
    private static final float PARTICLE_FADE_IN_ALPHA_TIME = 0.3f;
    private static final int PARTICLE_MIN_LIFETIME = 200;
    private static final int PARTICLE_MAX_LIFETIME = 300;

    private FireflyParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, TextureAtlasSprite sprite) {
        super(level, x, y, z, xa, ya, za, sprite);
        this.speedUpWhenYMotionIsBlocked = true;
        this.friction = 0.96f;
        this.quadSize *= 0.75f;
        this.yd *= (double)0.8f;
        this.xd *= (double)0.8f;
        this.zd *= (double)0.8f;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float a) {
        return (int)(255.0f * FireflyParticle.getFadeAmount(this.getLifetimeProgress((float)this.age + a), 0.1f, 0.3f));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isAir()) {
            this.remove();
            return;
        }
        this.setAlpha(FireflyParticle.getFadeAmount(this.getLifetimeProgress(this.age), 0.3f, 0.5f));
        if (this.random.nextFloat() > 0.95f || this.age == 1) {
            this.setParticleSpeed(-0.05f + 0.1f * this.random.nextFloat(), -0.05f + 0.1f * this.random.nextFloat(), -0.05f + 0.1f * this.random.nextFloat());
        }
    }

    private float getLifetimeProgress(float currentAge) {
        return Mth.clamp(currentAge / (float)this.lifetime, 0.0f, 1.0f);
    }

    private static float getFadeAmount(float lifetimeProgress, float fadeInTime, float fadeOutTime) {
        if (lifetimeProgress >= 1.0f - fadeInTime) {
            return (1.0f - lifetimeProgress) / fadeInTime;
        }
        if (lifetimeProgress <= fadeOutTime) {
            return lifetimeProgress / fadeOutTime;
        }
        return 1.0f;
    }

    @Environment(value=EnvType.CLIENT)
    public static class FireflyProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public FireflyProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            FireflyParticle particle = new FireflyParticle(level, x, y, z, 0.5 - random.nextDouble(), random.nextBoolean() ? yAux : -yAux, 0.5 - random.nextDouble(), this.sprite.get(random));
            particle.setLifetime(random.nextIntBetweenInclusive(200, 300));
            particle.scale(1.5f);
            particle.setAlpha(0.0f);
            return particle;
        }
    }
}

