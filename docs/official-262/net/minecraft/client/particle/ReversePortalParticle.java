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
import net.minecraft.client.particle.PortalParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class ReversePortalParticle
extends PortalParticle {
    private ReversePortalParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
        super(level, x, y, z, xd, yd, zd, sprite);
        this.quadSize *= 1.5f;
        this.lifetime = (int)(this.random.nextFloat() * 2.0f) + 60;
    }

    @Override
    public float getQuadSize(float a) {
        float s = 1.0f - ((float)this.age + a) / ((float)this.lifetime * 1.5f);
        return this.quadSize * s;
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
        float speedMultiplier = (float)this.age / (float)this.lifetime;
        this.x += this.xd * (double)speedMultiplier;
        this.y += this.yd * (double)speedMultiplier;
        this.z += this.zd * (double)speedMultiplier;
    }

    @Environment(value=EnvType.CLIENT)
    public static class ReversePortalProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public ReversePortalProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            ReversePortalParticle particle = new ReversePortalParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random));
            return particle;
        }
    }
}

