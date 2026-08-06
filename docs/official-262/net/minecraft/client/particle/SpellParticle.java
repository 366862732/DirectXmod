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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class SpellParticle
extends SingleQuadParticle {
    private static final RandomSource RANDOM = RandomSource.create();
    private final SpriteSet sprites;
    private float originalAlpha = 1.0f;

    private SpellParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, SpriteSet sprites) {
        super(level, x, y, z, 0.5 - RANDOM.nextDouble(), ya, 0.5 - RANDOM.nextDouble(), sprites.first());
        this.friction = 0.96f;
        this.gravity = -0.1f;
        this.speedUpWhenYMotionIsBlocked = true;
        this.sprites = sprites;
        this.yd *= (double)0.2f;
        if (xa == 0.0 && za == 0.0) {
            this.xd *= (double)0.1f;
            this.zd *= (double)0.1f;
        }
        this.quadSize *= 0.75f;
        this.lifetime = (int)(8.0 / ((double)this.random.nextFloat() * 0.8 + 0.2));
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
        if (this.isCloseToScopingPlayer()) {
            this.setAlpha(0.0f);
        }
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        this.alpha = this.isCloseToScopingPlayer() ? 0.0f : Mth.lerp(0.05f, this.alpha, this.originalAlpha);
    }

    @Override
    protected void setAlpha(float alpha) {
        super.setAlpha(alpha);
        this.originalAlpha = alpha;
    }

    private boolean isCloseToScopingPlayer() {
        Minecraft instance = Minecraft.getInstance();
        LocalPlayer player = instance.player;
        return player != null && player.getEyePosition().distanceToSqr(this.x, this.y, this.z) <= 9.0 && instance.options.getCameraType().isFirstPerson() && player.isScoping();
    }

    @Environment(value=EnvType.CLIENT)
    public static class InstantProvider
    implements ParticleProvider<SpellParticleOption> {
        private final SpriteSet sprite;

        public InstantProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SpellParticleOption options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            SpellParticle particle = new SpellParticle(level, x, y, z, xAux, yAux, zAux, this.sprite);
            particle.setColor(options.getRed(), options.getGreen(), options.getBlue());
            particle.setPower(options.getPower());
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class WitchProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public WitchProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            SpellParticle particle = new SpellParticle(level, x, y, z, xAux, yAux, zAux, this.sprite);
            float randBrightness = random.nextFloat() * 0.5f + 0.35f;
            particle.setColor(1.0f * randBrightness, 0.0f * randBrightness, 1.0f * randBrightness);
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class MobEffectProvider
    implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprite;

        public MobEffectProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(ColorParticleOption options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            SpellParticle particle = new SpellParticle(level, x, y, z, xAux, yAux, zAux, this.sprite);
            particle.setColor(options.getRed(), options.getGreen(), options.getBlue());
            particle.setAlpha(options.getAlpha());
            return particle;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Provider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new SpellParticle(level, x, y, z, xAux, yAux, zAux, this.sprite);
        }
    }
}

