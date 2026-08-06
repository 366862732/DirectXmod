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
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ScalableParticleOptionsBase;
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class DustParticleBase<T extends ScalableParticleOptionsBase>
extends SingleQuadParticle {
    private final SpriteSet sprites;

    protected DustParticleBase(ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, T options, SpriteSet sprites) {
        super(level, x, y, z, xAux, yAux, zAux, sprites.first());
        this.friction = 0.96f;
        this.speedUpWhenYMotionIsBlocked = true;
        this.sprites = sprites;
        this.xd *= (double)0.1f;
        this.yd *= (double)0.1f;
        this.zd *= (double)0.1f;
        this.quadSize *= 0.75f * ((ScalableParticleOptionsBase)options).getScale();
        int baseLifetime = (int)(8.0 / (this.random.nextDouble() * 0.8 + 0.2));
        this.lifetime = (int)Math.max((float)baseLifetime * ((ScalableParticleOptionsBase)options).getScale(), 1.0f);
        this.setSpriteFromAge(sprites);
    }

    protected float randomizeColor(float color, float baseFactor) {
        return (this.random.nextFloat() * 0.2f + 0.8f) * color * baseFactor;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public float getQuadSize(float a) {
        return this.quadSize * Mth.clamp(((float)this.age + a) / (float)this.lifetime * 32.0f, 0.0f, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }
}

