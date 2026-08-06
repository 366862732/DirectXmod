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
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class FallingLeavesParticle
extends SingleQuadParticle {
    private static final float ACCELERATION_SCALE = 0.0025f;
    private static final int INITIAL_LIFETIME = 300;
    private static final int CURVE_ENDPOINT_TIME = 300;
    private float rotSpeed;
    private final float spinAcceleration;
    private final float windBig;
    private final boolean swirl;
    private final boolean flowAway;
    private final double xaFlowScale;
    private final double zaFlowScale;
    private final double swirlPeriod;

    protected FallingLeavesParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite, float fallAcceleration, float sideAcceleration, boolean swirl, boolean flowAway, float scale, float startVelocity) {
        super(level, x, y, z, sprite);
        float size;
        this.rotSpeed = (float)Math.toRadians(this.random.nextBoolean() ? -30.0 : 30.0);
        this.spinAcceleration = (float)Math.toRadians(this.random.nextBoolean() ? -5.0 : 5.0);
        this.windBig = sideAcceleration;
        this.swirl = swirl;
        this.flowAway = flowAway;
        this.lifetime = 300;
        this.gravity = fallAcceleration * 1.2f * 0.0025f;
        this.quadSize = size = scale * (this.random.nextBoolean() ? 0.05f : 0.075f);
        this.setSize(size, size);
        this.friction = 1.0f;
        this.yd = -startVelocity;
        float particleRandom = this.random.nextFloat();
        this.xaFlowScale = Math.cos(Math.toRadians(particleRandom * 60.0f)) * (double)this.windBig;
        this.zaFlowScale = Math.sin(Math.toRadians(particleRandom * 60.0f)) * (double)this.windBig;
        this.swirlPeriod = Math.toRadians(1000.0f + particleRandom * 3000.0f);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.lifetime-- <= 0) {
            this.remove();
        }
        if (this.removed) {
            return;
        }
        float aliveTicks = 300 - this.lifetime;
        float relativeAge = Math.min(aliveTicks / 300.0f, 1.0f);
        double xa = 0.0;
        double za = 0.0;
        if (this.flowAway) {
            xa += this.xaFlowScale * Math.pow(relativeAge, 1.25);
            za += this.zaFlowScale * Math.pow(relativeAge, 1.25);
        }
        if (this.swirl) {
            xa += (double)relativeAge * Math.cos((double)relativeAge * this.swirlPeriod) * (double)this.windBig;
            za += (double)relativeAge * Math.sin((double)relativeAge * this.swirlPeriod) * (double)this.windBig;
        }
        this.xd += xa * (double)0.0025f;
        this.zd += za * (double)0.0025f;
        this.yd -= (double)this.gravity;
        this.rotSpeed += this.spinAcceleration / 20.0f;
        this.oRoll = this.roll;
        this.roll += this.rotSpeed / 20.0f;
        this.move(this.xd, this.yd, this.zd);
        if (this.onGround || this.lifetime < 299 && (this.xd == 0.0 || this.zd == 0.0)) {
            this.remove();
        }
        if (this.removed) {
            return;
        }
        this.xd *= (double)this.friction;
        this.yd *= (double)this.friction;
        this.zd *= (double)this.friction;
    }

    @Environment(value=EnvType.CLIENT)
    public static class TintedLeavesProvider
    implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public TintedLeavesProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            FallingLeavesParticle particle = new FallingLeavesParticle(level, x, y, z, this.sprites.get(random), 0.07f, 10.0f, true, false, 2.0f, 0.021f);
            particle.setColor(options.getRed(), options.getGreen(), options.getBlue());
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class PaleOakProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public PaleOakProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new FallingLeavesParticle(level, x, y, z, this.sprites.get(random), 0.07f, 10.0f, true, false, 2.0f, 0.021f);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class CherryProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public CherryProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new FallingLeavesParticle(level, x, y, z, this.sprites.get(random), 0.25f, 2.0f, false, true, 1.0f, 0.0f);
        }
    }
}

