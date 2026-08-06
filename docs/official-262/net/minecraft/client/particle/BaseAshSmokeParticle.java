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
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public abstract class BaseAshSmokeParticle
extends SingleQuadParticle {
    private final SpriteSet sprites;

    protected BaseAshSmokeParticle(ClientLevel level, double x, double y, double z, float dirX, float dirY, float dirZ, double xa, double ya, double za, float scale, SpriteSet sprites, float colorRandom, int maxLifetime, float gravity, boolean hasPhysics) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
        float col;
        this.friction = 0.96f;
        this.gravity = gravity;
        this.speedUpWhenYMotionIsBlocked = true;
        this.sprites = sprites;
        this.xd *= (double)dirX;
        this.yd *= (double)dirY;
        this.zd *= (double)dirZ;
        this.xd += xa;
        this.yd += ya;
        this.zd += za;
        this.rCol = col = this.random.nextFloat() * colorRandom;
        this.gCol = col;
        this.bCol = col;
        this.quadSize *= 0.75f * scale;
        this.lifetime = (int)((double)maxLifetime / ((double)this.random.nextFloat() * 0.8 + 0.2) * (double)scale);
        this.lifetime = Math.max(this.lifetime, 1);
        this.setSpriteFromAge(sprites);
        this.hasPhysics = hasPhysics;
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

