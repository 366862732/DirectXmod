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
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class FlyTowardsPositionParticle
extends SingleQuadParticle {
    private final double xStart;
    private final double yStart;
    private final double zStart;
    private final boolean isGlowing;
    private final Particle.LifetimeAlpha lifetimeAlpha;

    private FlyTowardsPositionParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
        this(level, x, y, z, xd, yd, zd, false, Particle.LifetimeAlpha.ALWAYS_OPAQUE, sprite);
    }

    private FlyTowardsPositionParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, boolean isGlowing, Particle.LifetimeAlpha lifetimeAlpha, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.isGlowing = isGlowing;
        this.lifetimeAlpha = lifetimeAlpha;
        this.setAlpha(lifetimeAlpha.startAlpha());
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.xStart = x;
        this.yStart = y;
        this.zStart = z;
        this.xo = x + xd;
        this.yo = y + yd;
        this.zo = z + zd;
        this.x = this.xo;
        this.y = this.yo;
        this.z = this.zo;
        this.quadSize = 0.1f * (this.random.nextFloat() * 0.5f + 0.2f);
        float br = this.random.nextFloat() * 0.6f + 0.4f;
        this.rCol = 0.9f * br;
        this.gCol = 0.9f * br;
        this.bCol = br;
        this.hasPhysics = false;
        this.lifetime = (int)(this.random.nextFloat() * 10.0f) + 30;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        if (this.lifetimeAlpha.isOpaque()) {
            return SingleQuadParticle.Layer.OPAQUE;
        }
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void move(double xa, double ya, double za) {
        this.setBoundingBox(this.getBoundingBox().move(xa, ya, za));
        this.setLocationFromBoundingbox();
    }

    @Override
    public int getLightCoords(float a) {
        if (this.isGlowing) {
            return LightCoordsUtil.withBlock(super.getLightCoords(a), 15);
        }
        float brightness = (float)this.age / (float)this.lifetime;
        brightness *= brightness;
        brightness *= brightness;
        return LightCoordsUtil.addSmoothBlockEmission(super.getLightCoords(a), brightness);
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
        float pos = (float)this.age / (float)this.lifetime;
        pos = 1.0f - pos;
        float pp = 1.0f - pos;
        pp *= pp;
        pp *= pp;
        this.x = this.xStart + this.xd * (double)pos;
        this.y = this.yStart + this.yd * (double)pos - (double)(pp * 1.2f);
        this.z = this.zStart + this.zd * (double)pos;
    }

    @Override
    public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime) {
        this.setAlpha(this.lifetimeAlpha.currentAlphaForAge(this.age, this.lifetime, partialTickTime));
        super.extract(particleTypeRenderState, camera, partialTickTime);
    }

    @Environment(value=EnvType.CLIENT)
    public static class VaultConnectionProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public VaultConnectionProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            FlyTowardsPositionParticle particle = new FlyTowardsPositionParticle(level, x, y, z, xAux, yAux, zAux, true, new Particle.LifetimeAlpha(0.0f, 0.6f, 0.25f, 1.0f), this.sprite.get(random));
            particle.scale(1.5f);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class NautilusProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public NautilusProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            FlyTowardsPositionParticle particle = new FlyTowardsPositionParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random));
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class EnchantProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public EnchantProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            FlyTowardsPositionParticle particle = new FlyTowardsPositionParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random));
            return particle;
        }
    }
}

