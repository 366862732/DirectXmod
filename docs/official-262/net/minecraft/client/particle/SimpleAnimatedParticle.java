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

@Environment(value=EnvType.CLIENT)
public abstract class SimpleAnimatedParticle
extends SingleQuadParticle {
    protected final SpriteSet sprites;
    private float fadeR;
    private float fadeG;
    private float fadeB;
    private boolean hasFade;

    protected SimpleAnimatedParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, float gravity) {
        super(level, x, y, z, sprites.first());
        this.friction = 0.91f;
        this.gravity = gravity;
        this.sprites = sprites;
    }

    public void setColor(int rgb) {
        float r = (float)((rgb & 0xFF0000) >> 16) / 255.0f;
        float g = (float)((rgb & 0xFF00) >> 8) / 255.0f;
        float b = (float)((rgb & 0xFF) >> 0) / 255.0f;
        float scale = 1.0f;
        this.setColor(r * 1.0f, g * 1.0f, b * 1.0f);
    }

    public void setFadeColor(int rgb) {
        this.fadeR = (float)((rgb & 0xFF0000) >> 16) / 255.0f;
        this.fadeG = (float)((rgb & 0xFF00) >> 8) / 255.0f;
        this.fadeB = (float)((rgb & 0xFF) >> 0) / 255.0f;
        this.hasFade = true;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        if (this.age > this.lifetime / 2) {
            this.setAlpha(1.0f - ((float)this.age - (float)(this.lifetime / 2)) / (float)this.lifetime);
            if (this.hasFade) {
                this.rCol += (this.fadeR - this.rCol) * 0.2f;
                this.gCol += (this.fadeG - this.gCol) * 0.2f;
                this.bCol += (this.fadeB - this.bCol) * 0.2f;
            }
        }
    }

    @Override
    public int getLightCoords(float a) {
        return 0xF000F0;
    }
}

