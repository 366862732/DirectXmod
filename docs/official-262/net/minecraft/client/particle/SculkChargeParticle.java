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
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class SculkChargeParticle
extends SingleQuadParticle {
    private final SpriteSet sprites;

    private SculkChargeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd, sprites.first());
        this.friction = 0.96f;
        this.sprites = sprites;
        this.scale(1.5f);
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public int getLightCoords(float a) {
        return LightCoordsUtil.withBlock(super.getLightCoords(a), 15);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    @Environment(value=EnvType.CLIENT)
    public record Provider(SpriteSet sprite) implements ParticleProvider<SculkChargeParticleOptions>
    {
        @Override
        public Particle createParticle(SculkChargeParticleOptions options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            SculkChargeParticle particle = new SculkChargeParticle(level, x, y, z, xAux, yAux, zAux, this.sprite);
            particle.setAlpha(1.0f);
            particle.setParticleSpeed(xAux, yAux, zAux);
            particle.oRoll = options.roll();
            particle.roll = options.roll();
            particle.setLifetime(random.nextInt(12) + 8);
            return particle;
        }
    }
}

