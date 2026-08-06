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
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class FlyStraightTowardsParticle
extends SingleQuadParticle {
    private final double xStart;
    private final double yStart;
    private final double zStart;
    private final int startColor;
    private final int endColor;

    private FlyStraightTowardsParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, int startColor, int endColor, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
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
        this.hasPhysics = false;
        this.lifetime = (int)(this.random.nextFloat() * 5.0f) + 25;
        this.startColor = startColor;
        this.endColor = endColor;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public void move(double xa, double ya, double za) {
    }

    @Override
    public int getLightCoords(float a) {
        return LightCoordsUtil.withBlock(super.getLightCoords(a), 15);
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
        float normalizedAge = (float)this.age / (float)this.lifetime;
        float posAlpha = 1.0f - normalizedAge;
        this.x = this.xStart + this.xd * (double)posAlpha;
        this.y = this.yStart + this.yd * (double)posAlpha;
        this.z = this.zStart + this.zd * (double)posAlpha;
        int color = ARGB.srgbLerp(normalizedAge, this.startColor, this.endColor);
        this.setColor((float)ARGB.red(color) / 255.0f, (float)ARGB.green(color) / 255.0f, (float)ARGB.blue(color) / 255.0f);
        this.setAlpha((float)ARGB.alpha(color) / 255.0f);
    }

    @Environment(value=EnvType.CLIENT)
    public static class OminousSpawnProvider
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public OminousSpawnProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            FlyStraightTowardsParticle particle = new FlyStraightTowardsParticle(level, x, y, z, xAux, yAux, zAux, -12210434, -1, this.sprite.get(random));
            particle.scale(Mth.randomBetween(level.getRandom(), 3.0f, 5.0f));
            return particle;
        }
    }
}

