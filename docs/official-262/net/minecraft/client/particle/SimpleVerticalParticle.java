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
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class SimpleVerticalParticle
extends SingleQuadParticle {
    private SimpleVerticalParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, TextureAtlasSprite sprite, boolean upwards) {
        super(level, x, y, z, xa, ya, za, sprite);
        this.xd = xa;
        this.zd = za;
        this.yd = ya;
        this.gravity = 0.0f;
        this.yd += upwards ? 0.03 : -0.03;
        this.quadSize *= this.random.nextFloat() * 0.6f + 0.5f;
        this.lifetime = 8;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Environment(value=EnvType.CLIENT)
    public record ResetMobGrowthProvider(SpriteSet sprite) implements ParticleProvider<SimpleParticleType>
    {
        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new SimpleVerticalParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random), true);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record PauseMobGrowthProvider(SpriteSet sprite) implements ParticleProvider<SimpleParticleType>
    {
        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new SimpleVerticalParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random), false);
        }
    }
}

