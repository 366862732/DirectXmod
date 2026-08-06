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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class PortalParticle
extends SingleQuadParticle {
    private final double xStart;
    private final double yStart;
    private final double zStart;

    protected PortalParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.x = x;
        this.y = y;
        this.z = z;
        this.xStart = this.x;
        this.yStart = this.y;
        this.zStart = this.z;
        this.quadSize = 0.1f * (this.random.nextFloat() * 0.2f + 0.5f);
        float br = this.random.nextFloat() * 0.6f + 0.4f;
        this.rCol = br * 0.9f;
        this.gCol = br * 0.3f;
        this.bCol = br;
        this.lifetime = (int)(this.random.nextFloat() * 10.0f) + 40;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public void move(double xa, double ya, double za) {
        this.setBoundingBox(this.getBoundingBox().move(xa, ya, za));
        this.setLocationFromBoundingbox();
    }

    @Override
    public float getQuadSize(float a) {
        float s = ((float)this.age + a) / (float)this.lifetime;
        s = 1.0f - s;
        s *= s;
        s = 1.0f - s;
        return this.quadSize * s;
    }

    @Override
    public int getLightCoords(float a) {
        float brightness = (float)this.age / (float)this.lifetime;
        brightness *= brightness;
        brightness *= brightness;
        return LightCoordsUtil.addSmoothBlockEmission(super.getLightCoords(a), brightness);
    }

    @Override
    public void tick() {
        float pos;
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        float a = pos = (float)this.age / (float)this.lifetime;
        pos = -pos + pos * pos * 2.0f;
        pos = 1.0f - pos;
        this.x = this.xStart + this.xd * (double)pos;
        this.y = this.yStart + this.yd * (double)pos + (double)(1.0f - a);
        this.z = this.zStart + this.zd * (double)pos;
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
            PortalParticle particle = new PortalParticle(level, x, y, z, xAux, yAux, zAux, this.sprite.get(random));
            return particle;
        }
    }
}

